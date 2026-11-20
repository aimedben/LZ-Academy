-- =====================================================================
-- LZ Academy · 0012 · Sécurité des supports de cours
-- · un enseignant ne publie que pour les groupes qu'il anime ;
-- · un élève ne peut pas créer de support ;
-- · un fichier Storage n'est lisible que si sa ligne `materials`
--   est elle-même visible (RLS de materials appliquée).
-- =====================================================================

drop policy if exists materials_owner_write on public.materials;
create policy materials_owner_write on public.materials
    for all to authenticated
    using (
        public.is_admin()
        or (
            public.current_teacher_id() is not null
            and uploaded_by = (select auth.uid())
            and group_id is not null
            and public.is_teacher_of_group(group_id)
        )
    )
    with check (
        public.is_admin()
        or (
            public.current_teacher_id() is not null
            and uploaded_by = (select auth.uid())
            and group_id is not null
            and public.is_teacher_of_group(group_id)
        )
    );

drop policy if exists materials_storage_read on storage.objects;
create policy materials_storage_read on storage.objects
    for select to authenticated
    using (
        bucket_id = 'materials'
        and exists (
            select 1
              from public.materials m
             where m.storage_path = storage.objects.name
               and (
                   m.is_published
                   or m.uploaded_by = (select auth.uid())
                   or public.is_admin()
               )
        )
    );
