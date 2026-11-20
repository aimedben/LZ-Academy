-- =====================================================================
-- LZ Academy · 0013 · Buckets de photos
-- · « teacher-photos » : public, limité à 2 Mo, images uniquement
--   — dossier imposé : {auth.uid()}/{teacher_id}/{fichier}
-- · « avatars » : public, limité à 2 Mo, images uniquement
--   — dossier imposé : {auth.uid()}/{fichier} (profiles.avatar_url)
-- · RPC update_teacher_photo : vérifie le fichier, publie l'URL.
-- Les fichiers restent dans Storage (1 Go offert) — jamais en base.
-- =====================================================================

-- ---------------------------------------------------------------------
-- 1. Buckets
-- ---------------------------------------------------------------------
insert into storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
values
    ('teacher-photos', 'teacher-photos', true, 2097152,
     array['image/png', 'image/jpeg', 'image/webp']),
    ('avatars', 'avatars', true, 2097152,
     array['image/png', 'image/jpeg', 'image/webp'])
on conflict (id) do update
   set public             = excluded.public,
       file_size_limit    = excluded.file_size_limit,
       allowed_mime_types = excluded.allowed_mime_types;

-- ---------------------------------------------------------------------
-- 2. Policies lectures conventionnées (API JSON du Storage ; les URLs
--    publiques interrogent directement le CDN, sans policy)
-- ---------------------------------------------------------------------
drop policy if exists teacher_photos_read on storage.objects;
create policy teacher_photos_read on storage.objects
    for select to anon, authenticated
    using (bucket_id = 'teacher-photos');

drop policy if exists avatars_read on storage.objects;
create policy avatars_read on storage.objects
    for select to anon, authenticated
    using (bucket_id = 'avatars');

-- ---------------------------------------------------------------------
-- 3. Écritures : chaque utilisateur écrit uniquement dans son dossier.
--    teacher-photos : {uid}/{teacher_id}/… , le teacher_id doit être celui
--    du profil connecté (l'admin passe par le service role / console).
--    Stack « upsert » : SELECT + INSERT + UPDATE + DELETE.
-- ---------------------------------------------------------------------
drop policy if exists teacher_photos_write on storage.objects;
create policy teacher_photos_write on storage.objects
    for insert to authenticated
    with check (
        bucket_id = 'teacher-photos'
        and (storage.foldername(name))[1] = (select auth.uid())::text
        and exists (
            select 1
              from public.teachers t
             where t.id::text = (storage.foldername(name))[2]
               and t.profile_id = (select auth.uid())
        )
    );

drop policy if exists teacher_photos_update on storage.objects;
create policy teacher_photos_update on storage.objects
    for update to authenticated
    using (
        bucket_id = 'teacher-photos'
        and (storage.foldername(name))[1] = (select auth.uid())::text
        and exists (
            select 1
              from public.teachers t
             where t.id::text = (storage.foldername(name))[2]
               and t.profile_id = (select auth.uid())
        )
    )
    with check (
        bucket_id = 'teacher-photos'
        and (storage.foldername(name))[1] = (select auth.uid())::text
        and exists (
            select 1
              from public.teachers t
             where t.id::text = (storage.foldername(name))[2]
               and t.profile_id = (select auth.uid())
        )
    );

drop policy if exists teacher_photos_delete on storage.objects;
create policy teacher_photos_delete on storage.objects
    for delete to authenticated
    using (
        bucket_id = 'teacher-photos'
        and (storage.foldername(name))[1] = (select auth.uid())::text
        and exists (
            select 1
              from public.teachers t
             where t.id::text = (storage.foldername(name))[2]
               and t.profile_id = (select auth.uid())
        )
    );

drop policy if exists avatars_write on storage.objects;
create policy avatars_write on storage.objects
    for all to authenticated
    using (
        bucket_id = 'avatars'
        and (storage.foldername(name))[1] = (select auth.uid())::text
    )
    with check (
        bucket_id = 'avatars'
        and (storage.foldername(name))[1] = (select auth.uid())::text
    );

-- ---------------------------------------------------------------------
-- 4. RPC : l'enseignant enregistre la photo qu'il vient d'envoyer.
--    Vérifie l'existence de l'objet, puis stocke l'URL publique.
--    Défacultative : p_photo_path null → retire la photo.
-- ---------------------------------------------------------------------
create or replace function public.update_teacher_photo(p_photo_path text)
returns text
language plpgsql
security definer
set search_path = public
as $$
declare
    v_teacher_id uuid;
    v_exists     boolean;
    v_url        text := 'https://qxnuuhjjuzudxyyleplw.supabase.co/storage/v1/object/public/';
begin
    select t.id into v_teacher_id
      from public.teachers t
     where t.profile_id = (select auth.uid())
       and t.is_active
     limit 1;

    if v_teacher_id is null then
        raise exception 'Profil enseignant introuvable pour cet utilisateur.';
    end if;

    if coalesce(btrim(p_photo_path), '') = '' then
        update public.teachers
           set photo_url = null
         where id = v_teacher_id;
        return null;
    end if;

    -- Le dossier du fichier doit être {uid}/{teacher_id}/…
    v_exists := exists (
        select 1
          from storage.objects o
         where o.bucket_id = 'teacher-photos'
           and o.name = p_photo_path
           and (storage.foldername(o.name))[1] = (select auth.uid())::text
           and (storage.foldername(o.name))[2] = v_teacher_id::text
    );

    if not v_exists then
        raise exception 'Fichier photo introuvable ou hors de votre dossier.';
    end if;

    v_url := v_url || p_photo_path;

    update public.teachers
       set photo_url = v_url
     where id = v_teacher_id;

    return v_url;
end;
$$;

revoke execute on function public.update_teacher_photo(text) from public, anon;
grant  execute on function public.update_teacher_photo(text) to authenticated;
