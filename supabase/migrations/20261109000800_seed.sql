-- =====================================================================
-- LZ Academy · 0008 · Données de référence & jeu de démonstration
-- Année scolaire, niveaux, matières, 21 enseignants, salles, packs,
-- groupes de M. Karim, élèves, inscriptions, abonnements, paiements,
-- supports, annonces, diffusions et rotation de la vitrine.
--
-- Le fichier est idempotent : il peut être rejoué sans doublon.
-- Les identifiants des enseignants et des élèves sont déterministes
-- (md5('lz:...')::uuid) afin que les liens restent stables.
-- =====================================================================

-- ---------------------------------------------------------------------
-- Année scolaire en cours
-- ---------------------------------------------------------------------
insert into public.academic_years (label, starts_on, ends_on, is_current)
values ('2025-2026', date '2025-09-01', date '2026-06-30', true)
on conflict (label) do nothing;

-- ---------------------------------------------------------------------
-- Niveaux
-- ---------------------------------------------------------------------
insert into public.levels (code, name, cycle, order_index)
values
    ('3AM',     '3ème Année Moyenne',   'Moyen',      0),
    ('1AS',     '1AS Tronc Commun',     'Secondaire', 1),
    ('2AS',     '2AS Sciences',         'Secondaire', 2),
    ('3AS-SCI', '3AS Scientifique',     'Secondaire', 3),
    ('3AS-MT',  '3AS Math-Technique',   'Secondaire', 4)
on conflict (code) do nothing;

-- ---------------------------------------------------------------------
-- Matières (15)
-- ---------------------------------------------------------------------
insert into public.subjects (code, name, accent)
values
    ('MATH',  'Mathématiques',       'green'),
    ('PHY',   'Physique-Chimie',     'violet'),
    ('SVT',   'Sciences Naturelles', 'green'),
    ('FR',    'Français',            'orange'),
    ('ANG',   'Anglais',             'blue'),
    ('PHILO', 'Philosophie',         'violet'),
    ('AR',    'Arabe',               'green'),
    ('HG',    'Histoire-Géographie', 'orange'),
    ('INFO',  'Informatique',        'blue'),
    ('ECO',   'Économie-Gestion',    'orange'),
    ('ESP',   'Espagnol',            'orange'),
    ('ALL',   'Allemand',            'blue'),
    ('COMPTA','Comptabilité',        'violet'),
    ('METHO', 'Méthodologie',        'green'),
    ('SOUTIEN','Soutien scolaire',   'blue')
on conflict (code) do nothing;

-- ---------------------------------------------------------------------
-- Salles
-- ---------------------------------------------------------------------
insert into public.rooms (name, building, capacity)
values
    ('Salle A1',            'Bâtiment principal', 30),
    ('Salle B2',            'Bâtiment principal', 25),
    ('Salle C3',            'Bâtiment principal', 25),
    ('Salle 101',           'Bâtiment principal', 22),
    ('Salle 102',           'Bâtiment principal', 20),
    ('Salle 103',           'Bâtiment principal', 18),
    ('Salle Informatique',  'Bâtiment annexe',    20),
    ('Amphithéâtre LZ',     'Bâtiment principal', 80)
on conflict (name) do nothing;

-- ---------------------------------------------------------------------
-- Formules (packs)
-- ---------------------------------------------------------------------
insert into public.packs (name, description, price_dzd, sessions_count, duration_months)
values
    ('Pack Annuel BAC Réussite',
     'Toutes les matières, polycopiés et 3 BAC blancs.',
     120000, 108, 9),
    ('Pack Trimestriel Intensif',
     '4 matières au choix et suivi personnalisé.',
     42000, 48, 3),
    ('Pack Mensuel Standard',
     '2 matières, 8 séances par matière.',
     15000, 16, 1),
    ('Séances Particulières à la carte',
     'Coaching individuel avec un professeur certifié.',
     3000, 1, null),
    ('Carnet 4 séances',
     'Carnet souple de 4 séances, valable un trimestre.',
     6000, 4, 3)
on conflict (name) do nothing;

-- ---------------------------------------------------------------------
-- Enseignants (21) — miroir exact de TeacherSeed.kt
-- ---------------------------------------------------------------------
insert into public.teachers (
    id, full_name, photo_url, professional_description, years_of_experience,
    is_featured_on_student_home, accepts_student_messages,
    featured_rotation_priority, is_active
)
select
    md5('lz:teacher:' || v.slug)::uuid,
    v.full_name,
    null,
    v.description,
    v.years,
    v.featured,
    v.accepts,
    v.priority,
    true
from (values
    ('t-karim', 'M. Karim Lebbah',
     'Agrégé de l''ENS, spécialiste de l''analyse et des probabilités pour le BAC.',
     12, true, true, 100),
    ('t-amina', 'Mme Amina Boudjema',
     'Ancienne enseignante au lycée d''excellence, passionnée par les sciences expérimentales.',
     9, true, true, 95),
    ('t-yacine', 'M. Yacine Hamdi',
     'Biologiste de formation, il prépare les élèves aux épreuves expérimentales du BAC.',
     7, true, true, 90),
    ('t-nadia', 'Mme Nadia Bensalem',
     'Spécialiste de la dissertation et du commentaire de texte, méthode progressive.',
     14, true, false, 85),
    ('t-reda', 'M. Reda Said',
     'Certifié Cambridge, il travaille l''oral et la compréhension écrite en petits groupes.',
     8, true, false, 80),
    ('t-samir', 'M. Samir Belkacem',
     'Professeur de philosophie, il accompagne les élèves dans la construction du raisonnement.',
     11, true, true, 75),
    ('t-leila', 'Mme Leila Zerrouki',
     'Agrégée de langue arabe, attentive à l''expression écrite et à la grammaire.',
     10, true, true, 70),
    ('t-omar', 'M. Omar Ferhat',
     'Historien, il relie les programmes au monde contemporain pour donner du sens.',
     6, true, false, 65),
    ('t-ines', 'Mme Ines Belaid',
     'Ingénieure logiciel, elle initie les élèves à l''algorithmique et à la bureautique.',
     5, true, true, 60),
    ('t-mehdi', 'M. Mehdi Rahmani',
     'Consultant en gestion, il illustre le cours par des cas concrets d''entreprise.',
     7, true, true, 55),
    ('t-samia', 'Mme Samia Kaci',
     'Professeure d''espagnol, elle privilégie la conversation et la culture hispanique.',
     9, false, true, 50),
    ('t-fouad', 'M. Fouad Benmoussa',
     'Enseignant d''allemand, il prépare aux certifications et aux échanges scolaires.',
     6, false, false, 45),
    ('t-hakim', 'M. Hakim Cherifi',
     'Expert-comptable, il rend la comptabilité générale claire et méthodique.',
     13, true, true, 40),
    ('t-rania', 'Mme Rania Bouzid',
     'Spécialiste du soutien en mathématiques, elle reprend les bases sans pression.',
     4, true, true, 35),
    ('t-sofiane', 'M. Sofiane Merabet',
     'Passionné de chimie organique, il prépare des exercices corrigés chaque semaine.',
     8, true, true, 30),
    ('t-djamila', 'Mme Djamila Saidi',
     'Professeure de SVT, elle accompagne les travaux pratiques et les exposés.',
     15, true, false, 25),
    ('t-bilal', 'M. Bilal Mansouri',
     'Écrivain et enseignant, il transmet le goût de la lecture et de l''argumentation.',
     3, true, true, 20),
    ('t-assia', 'Mme Assia Larbi',
     'Traductrice et professeure, elle cible la grammaire utile aux examens.',
     6, true, true, 15),
    ('t-mourad', 'M. Mourad Benyahia',
     'Développeur et formateur, il enseigne la logique de programmation par projets.',
     5, true, true, 10),
    ('t-nour', 'Mme Nour El Houda Amrani',
     'Coach scolaire, elle aide les élèves à organiser leurs révisions et leur stress.',
     4, true, true, 5),
    ('t-karima', 'Mme Karima Taleb',
     'Spécialiste du soutien individualisé, elle adapte le rythme à chaque élève.',
     11, false, true, 0)
) as v(slug, full_name, description, years, featured, accepts, priority)
on conflict (id) do nothing;

-- Matières enseignées (une matière principale par enseignant)
insert into public.teacher_subjects (teacher_id, subject_id, is_primary)
select t.id, s.id, true
from (values
    ('M. Karim Lebbah',        'Mathématiques'),
    ('Mme Amina Boudjema',     'Physique-Chimie'),
    ('M. Yacine Hamdi',        'Sciences Naturelles'),
    ('Mme Nadia Bensalem',     'Français'),
    ('M. Reda Said',           'Anglais'),
    ('M. Samir Belkacem',      'Philosophie'),
    ('Mme Leila Zerrouki',     'Arabe'),
    ('M. Omar Ferhat',         'Histoire-Géographie'),
    ('Mme Ines Belaid',        'Informatique'),
    ('M. Mehdi Rahmani',       'Économie-Gestion'),
    ('Mme Samia Kaci',         'Espagnol'),
    ('M. Fouad Benmoussa',     'Allemand'),
    ('M. Hakim Cherifi',       'Comptabilité'),
    ('Mme Rania Bouzid',       'Mathématiques'),
    ('M. Sofiane Merabet',     'Physique-Chimie'),
    ('Mme Djamila Saidi',      'Sciences Naturelles'),
    ('M. Bilal Mansouri',      'Français'),
    ('Mme Assia Larbi',        'Anglais'),
    ('M. Mourad Benyahia',     'Informatique'),
    ('Mme Nour El Houda Amrani','Méthodologie'),
    ('Mme Karima Taleb',       'Soutien scolaire')
) as v(full_name, subject_name)
join public.teachers t on t.full_name = v.full_name
join public.subjects s on s.name = v.subject_name
on conflict (teacher_id, subject_id) do nothing;

-- Niveaux enseignés
insert into public.teacher_levels (teacher_id, level_id)
select t.id, l.id
from (values
    ('M. Karim Lebbah', '3AS Scientifique'),
    ('M. Karim Lebbah', '3AS Math-Technique'),
    ('M. Karim Lebbah', '2AS Sciences'),
    ('Mme Amina Boudjema', '3AS Scientifique'),
    ('Mme Amina Boudjema', '2AS Sciences'),
    ('M. Yacine Hamdi', '3AS Scientifique'),
    ('M. Yacine Hamdi', '1AS Tronc Commun'),
    ('Mme Nadia Bensalem', '3AS Scientifique'),
    ('Mme Nadia Bensalem', '2AS Sciences'),
    ('M. Reda Said', '3AS Scientifique'),
    ('M. Reda Said', '1AS Tronc Commun'),
    ('M. Samir Belkacem', '3AS Scientifique'),
    ('M. Samir Belkacem', '3AS Math-Technique'),
    ('Mme Leila Zerrouki', '2AS Sciences'),
    ('Mme Leila Zerrouki', '1AS Tronc Commun'),
    ('M. Omar Ferhat', '3AS Scientifique'),
    ('M. Omar Ferhat', '2AS Sciences'),
    ('Mme Ines Belaid', '2AS Sciences'),
    ('Mme Ines Belaid', '1AS Tronc Commun'),
    ('M. Mehdi Rahmani', '3AS Scientifique'),
    ('M. Mehdi Rahmani', '2AS Sciences'),
    ('Mme Samia Kaci', '2AS Sciences'),
    ('Mme Samia Kaci', '1AS Tronc Commun'),
    ('M. Fouad Benmoussa', '2AS Sciences'),
    ('M. Fouad Benmoussa', '1AS Tronc Commun'),
    ('M. Hakim Cherifi', '3AS Scientifique'),
    ('M. Hakim Cherifi', '2AS Sciences'),
    ('Mme Rania Bouzid', '2AS Sciences'),
    ('Mme Rania Bouzid', '1AS Tronc Commun'),
    ('M. Sofiane Merabet', '3AS Math-Technique'),
    ('M. Sofiane Merabet', '3AS Scientifique'),
    ('Mme Djamila Saidi', '3AS Scientifique'),
    ('Mme Djamila Saidi', '2AS Sciences'),
    ('M. Bilal Mansouri', '2AS Sciences'),
    ('M. Bilal Mansouri', '1AS Tronc Commun'),
    ('Mme Assia Larbi', '3AS Scientifique'),
    ('Mme Assia Larbi', '2AS Sciences'),
    ('M. Mourad Benyahia', '3AS Math-Technique'),
    ('M. Mourad Benyahia', '2AS Sciences'),
    ('Mme Nour El Houda Amrani', '3AS Scientifique'),
    ('Mme Nour El Houda Amrani', '3AS Math-Technique'),
    ('Mme Nour El Houda Amrani', '2AS Sciences'),
    ('Mme Karima Taleb', '1AS Tronc Commun'),
    ('Mme Karima Taleb', '2AS Sciences')
) as v(full_name, level_name)
join public.teachers t on t.full_name = v.full_name
join public.levels l on l.name = v.level_name
on conflict (teacher_id, level_id) do nothing;

-- ---------------------------------------------------------------------
-- Groupes de M. Karim (année en cours)
-- ---------------------------------------------------------------------
insert into public.groups (name, level_id, teacher_id, default_room_id, academic_year_id, capacity)
select v.name, l.id, t.id, r.id, y.id, v.capacity
from (values
    ('3AS - Sciences Expérimentales (G1)', '3AS-SCI', 't-karim', 'Salle A1',        30),
    ('3AS - Math-Technique (G2)',           '3AS-MT',  't-karim', 'Salle B2',        25),
    ('2AS - Tronc Commun Sciences (G3)',    '2AS',     't-karim', 'Salle C3',        25),
    ('BAC Intensif Week-end',               '3AS-SCI', 't-karim', 'Amphithéâtre LZ', 80)
) as v(name, level_code, teacher_slug, room_name, capacity)
join public.academic_years y on y.is_current
join public.levels l on l.code = v.level_code
join public.teachers t on t.id = md5('lz:teacher:' || v.teacher_slug)::uuid
join public.rooms r on r.name = v.room_name
on conflict (name, academic_year_id) do nothing;

-- Créneaux hebdomadaires
insert into public.group_schedule_slots (group_id, weekday, starts_at, ends_at, room_id)
select g.id, v.weekday, v.starts_at::time, v.ends_at::time, r.id
from (values
    ('3AS - Sciences Expérimentales (G1)', 1, '16:00', '18:00', 'Salle A1'),
    ('3AS - Sciences Expérimentales (G1)', 3, '16:00', '18:00', 'Salle A1'),
    ('3AS - Math-Technique (G2)',           2, '18:00', '20:00', 'Salle B2'),
    ('3AS - Math-Technique (G2)',           4, '18:00', '20:00', 'Salle B2'),
    ('2AS - Tronc Commun Sciences (G3)',    3, '14:00', '16:00', 'Salle C3'),
    ('2AS - Tronc Commun Sciences (G3)',    6, '14:00', '16:00', 'Salle C3'),
    ('BAC Intensif Week-end',               6, '09:00', '13:00', 'Amphithéâtre LZ')
) as v(group_name, weekday, starts_at, ends_at, room_name)
join public.groups g on g.name = v.group_name
join public.rooms r on r.name = v.room_name
where not exists (
    select 1 from public.group_schedule_slots x
     where x.group_id = g.id
       and x.weekday = v.weekday
       and x.starts_at = v.starts_at::time
);

-- ---------------------------------------------------------------------
-- Élèves (12)
-- ---------------------------------------------------------------------
insert into public.students (
    id, matricule, full_name, level_id, phone, guardian_name, guardian_phone
)
select
    md5('lz:student:' || v.slug)::uuid,
    v.matricule,
    v.full_name,
    l.id,
    v.phone,
    v.guardian_name,
    v.guardian_phone
from (values
    ('s1',  'LZ-894', 'Rayan Benali',      '3AS-SCI', '+213 550 12 34 56', 'M. Benali',  '+213 661 00 11 22'),
    ('s2',  'LZ-895', 'Amine Khelifi',     '3AS-MT',  '+213 661 22 33 44', 'Mme Khelifi','+213 770 33 44 55'),
    ('s3',  'LZ-896', 'Sarah Boumedienne', '2AS',     '+213 770 99 88 77', 'M. Boumedienne', '+213 551 55 66 77'),
    ('s4',  'LZ-897', 'Yacine Mansouri',   '3AS-SCI', '+213 551 44 55 66', 'Mme Mansouri', '+213 662 66 77 88'),
    ('s5',  'LZ-898', 'Lina Cherif',       '1AS',     '+213 662 11 22 33', 'M. Cherif',   '+213 770 88 99 00'),
    ('s6',  'LZ-899', 'Mehdi Zaidi',       '3AS-SCI', '+213 555 10 20 30', 'Mme Zaidi',   '+213 661 10 20 30'),
    ('s7',  'LZ-900', 'Nour Belhadj',      '3AS-MT',  '+213 666 40 50 60', 'M. Belhadj',  '+213 770 40 50 60'),
    ('s8',  'LZ-901', 'Walid Benaissa',    '1AS',     '+213 557 70 80 90', 'Mme Benaissa','+213 551 70 80 90'),
    ('s9',  'LZ-902', 'Amine Benali',      '3AS-SCI', '+213 550 11 22 33', 'M. Benali',   '+213 661 11 22 33'),
    ('s10', 'LZ-903', 'Meriem Haddad',     '2AS',     '+213 660 22 33 44', 'Mme Haddad',  '+213 770 22 33 44'),
    ('s11', 'LZ-904', 'Ilyes Boudiaf',     '3AS-MT',  '+213 550 44 55 66', 'M. Boudiaf',  '+213 661 44 55 66'),
    ('s12', 'LZ-905', 'Anis Cherif',       '1AS',     '+213 770 11 22 33', 'Mme Cherif',  '+213 551 11 22 33')
) as v(slug, matricule, full_name, level_code, phone, guardian_name, guardian_phone)
join public.levels l on l.code = v.level_code
on conflict (matricule) do nothing;

-- ---------------------------------------------------------------------
-- Inscriptions aux groupes
-- ---------------------------------------------------------------------
insert into public.enrollments (student_id, group_id, joined_on)
select s.id, g.id, current_date - 30
from (values
    ('s1',  '3AS - Sciences Expérimentales (G1)'),
    ('s4',  '3AS - Sciences Expérimentales (G1)'),
    ('s6',  '3AS - Sciences Expérimentales (G1)'),
    ('s9',  '3AS - Sciences Expérimentales (G1)'),
    ('s2',  '3AS - Math-Technique (G2)'),
    ('s7',  '3AS - Math-Technique (G2)'),
    ('s11', '3AS - Math-Technique (G2)'),
    ('s3',  '2AS - Tronc Commun Sciences (G3)'),
    ('s10', '2AS - Tronc Commun Sciences (G3)'),
    ('s1',  'BAC Intensif Week-end'),
    ('s4',  'BAC Intensif Week-end'),
    ('s6',  'BAC Intensif Week-end'),
    ('s9',  'BAC Intensif Week-end'),
    ('s2',  'BAC Intensif Week-end'),
    ('s7',  'BAC Intensif Week-end'),
    ('s11', 'BAC Intensif Week-end')
) as v(student_slug, group_name)
join public.students s on s.id = md5('lz:student:' || v.student_slug)::uuid
join public.groups g on g.name = v.group_name
on conflict (student_id, group_id) do nothing;

-- ---------------------------------------------------------------------
-- Abonnements
-- ---------------------------------------------------------------------
insert into public.subscriptions (
    student_id, pack_id, starts_on, ends_on,
    sessions_total, sessions_used, price_dzd, status
)
select
    s.id,
    p.id,
    current_date - 30,
    case p.duration_months
        when 9 then current_date + 240
        when 3 then current_date + 60
        when 1 then current_date + 20
        else current_date + 90
    end,
    v.total,
    v.used,
    p.price_dzd,
    case when v.used >= v.total
         then 'exhausted'::public.subscription_status
         else 'active'::public.subscription_status
    end
from (values
    ('s1',  'Pack Annuel BAC Réussite',       108, 40),
    ('s2',  'Pack Trimestriel Intensif',       48, 12),
    ('s3',  'Pack Mensuel Standard',           16,  6),
    ('s4',  'Carnet 4 séances',                 4,  1),
    ('s5',  'Carnet 4 séances',                 4,  4),
    ('s6',  'Carnet 4 séances',                 4,  2),
    ('s7',  'Pack Mensuel Standard',           16, 10),
    ('s8',  'Carnet 4 séances',                 4,  0),
    ('s9',  'Carnet 4 séances',                 4,  3),
    ('s10', 'Pack Trimestriel Intensif',       48, 20),
    ('s11', 'Carnet 4 séances',                 4,  2),
    ('s12', 'Carnet 4 séances',                 4,  1)
) as v(student_slug, pack_name, total, used)
join public.students s on s.id = md5('lz:student:' || v.student_slug)::uuid
join public.packs p on p.name = v.pack_name
where not exists (
    select 1 from public.subscriptions x
     where x.student_id = s.id
       and x.pack_id = p.id
       and x.starts_on = current_date - 30
);

-- ---------------------------------------------------------------------
-- Paiements
-- ---------------------------------------------------------------------
insert into public.payments (
    student_id, subscription_id, amount_dzd, method, status,
    reference, due_on, paid_at
)
select
    s.id,
    sub.id,
    v.amount,
    v.method::public.payment_method,
    v.status::public.payment_status,
    v.reference,
    current_date + v.due_offset,
    case when v.status = 'paid'
         then now() - (v.paid_days || ' days')::interval
         else null
    end
from (values
    ('s1',  120000, 'ccp',       'paid',    'CCP-2026-0001',   0, 20),
    ('s2',   42000, 'baridimob', 'paid',    'BAR-2026-0002',   0, 18),
    ('s3',   15000, 'cash',      'pending', 'CASH-2026-0003',  5,  0),
    ('s4',    6000, 'ccp',       'paid',    'CCP-2026-0004',   0, 15),
    ('s5',    6000, 'cash',      'overdue', 'CASH-2026-0005', -10, 0),
    ('s6',    6000, 'baridimob', 'paid',    'BAR-2026-0006',   0, 12),
    ('s7',   15000, 'ccp',       'paid',    'CCP-2026-0007',   0, 10),
    ('s8',    6000, 'cash',      'pending', 'CASH-2026-0008',  7,  0),
    ('s9',    6000, 'ccp',       'paid',    'CCP-2026-0009',   0,  8),
    ('s10',  42000, 'bank',      'overdue', 'BNK-2026-0010', -15, 0),
    ('s11',   6000, 'baridimob', 'paid',    'BAR-2026-0011',   0,  6),
    ('s12',   6000, 'cash',      'paid',    'CASH-2026-0012',  0,  4)
) as v(student_slug, amount, method, status, reference, due_offset, paid_days)
join public.students s on s.id = md5('lz:student:' || v.student_slug)::uuid
join lateral (
    select x.id from public.subscriptions x
     where x.student_id = s.id
     order by x.created_at desc
     limit 1
) sub on true
where not exists (
    select 1 from public.payments p where p.reference = v.reference
);

-- ---------------------------------------------------------------------
-- Supports de cours (métadonnées ; fichiers dans le bucket « materials »)
-- ---------------------------------------------------------------------
insert into public.materials (
    title, description, kind, storage_path, size_bytes,
    subject_id, group_id, is_published, published_at
)
select
    v.title, v.description, v.kind::public.file_kind, v.storage_path,
    v.size_bytes, s.id, g.id, true,
    now() - (v.age_hours || ' hours')::interval
from (values
    ('Chapitre 1 - Fonctions',
     'Cours complet et exemples corrigés sur les fonctions et dérivées.',
     'pdf', 'seed/karim/g1/chapitre-1-fonctions.pdf', 2516582,
     'Mathématiques', '3AS - Sciences Expérimentales (G1)', 2),
    ('TD - Dérivées',
     'Feuille de travaux dirigés avec corrigé détaillé.',
     'doc', 'seed/karim/g1/td-derivees.docx', 1153433,
     'Mathématiques', '3AS - Sciences Expérimentales (G1)', 3),
    ('Résumé - Intégrales',
     'Présentation de synthèse pour les révisions du BAC.',
     'slides', 'seed/karim/g1/resume-integrales.pptx', 911360,
     'Mathématiques', '3AS - Sciences Expérimentales (G1)', 26),
    ('TP - Dosage acide-base',
     'Protocole de travaux pratiques et grille d''évaluation.',
     'pdf', 'seed/karim/g2/tp-dosage.pdf', 1887436,
     'Physique-Chimie', '3AS - Math-Technique (G2)', 50)
) as v(title, description, kind, storage_path, size_bytes, subject_name, group_name, age_hours)
join public.subjects s on s.name = v.subject_name
join public.groups g on g.name = v.group_name
where not exists (
    select 1 from public.materials m where m.storage_path = v.storage_path
);

-- ---------------------------------------------------------------------
-- Annonces
-- ---------------------------------------------------------------------
insert into public.announcements (
    title, body, audience, level_id, group_id, is_pinned, published_at
)
select
    'Nouveau résumé disponible',
    'M. Lebbah a publié le résumé du chapitre « Fonctions et dérivées ». Retrouvez-le dans la rubrique Cours, dossier Résumés.',
    'group', null, g.id, true, now() - interval '2 hours'
from public.groups g
where g.name = '3AS - Sciences Expérimentales (G1)'
  and not exists (select 1 from public.announcements a where a.title = 'Nouveau résumé disponible');

insert into public.announcements (
    title, body, audience, level_id, group_id, is_pinned, published_at
)
select
    'Séance de physique décalée',
    'La séance de physique de lundi débutera à 18:15 au lieu de 18:00. Merci d''arriver quelques minutes à l''avance.',
    'all'::public.audience, null, null, false, now() - interval '1 day'
where not exists (
    select 1 from public.announcements a where a.title = 'Séance de physique décalée'
);

insert into public.announcements (
    title, body, audience, level_id, group_id, is_pinned, published_at
)
select
    'Votre forfait expire bientôt',
    'Il vous reste 3 séances sur 4. Pensez à renouveler votre abonnement avant la fin du mois pour garder votre place dans le groupe.',
    'students'::public.audience, null, null, false, now() - interval '2 days'
where not exists (
    select 1 from public.announcements a where a.title = 'Votre forfait expire bientôt'
);

insert into public.announcements (
    title, body, audience, level_id, group_id, is_pinned, published_at
)
select
    'Bienvenue sur LZ Academy',
    'Retrouvez ici votre planning, vos cours, vos paiements et toutes les annonces du centre.',
    'all'::public.audience, null, null, false, now() - interval '30 days'
where not exists (
    select 1 from public.announcements a where a.title = 'Bienvenue sur LZ Academy'
);

-- ---------------------------------------------------------------------
-- Diffusions (historique administration)
-- ---------------------------------------------------------------------
insert into public.broadcasts (
    title, body, audience, channel, recipients_count, delivered_count, sent_at
)
select
    v.title, v.body, v.audience::public.audience, v.channel::public.broadcast_channel,
    v.recipients, v.delivered, now() - (v.age_days || ' days')::interval
from (values
    ('Rentrée du 2e trimestre',
     'Les cours reprennent le lundi 5 janvier. Consultez votre planning mis à jour.',
     'all', 'push', 320, 318, 3),
    ('Rappel paiements',
     'Certains abonnements arrivent à échéance. Merci de régulariser auprès de l''administration.',
     'students', 'push_sms', 287, 279, 9)
) as v(title, body, audience, channel, recipients, delivered, age_days)
where not exists (
    select 1 from public.broadcasts b where b.title = v.title
);

-- ---------------------------------------------------------------------
-- Paramètres de l'académie
-- ---------------------------------------------------------------------
insert into public.academy_settings (
    id, name, address, phone, email, timezone, academic_year_id
)
select
    1,
    'LZ Academy',
    'Chéraga, Alger',
    '+213 550 00 00 00',
    'contact@lzacademy.dz',
    'Africa/Algiers',
    y.id
from public.academic_years y
where y.is_current
on conflict (id) do update
   set name             = excluded.name,
       address          = excluded.address,
       phone            = excluded.phone,
       email            = excluded.email,
       timezone         = excluded.timezone,
       academic_year_id = excluded.academic_year_id;

-- ---------------------------------------------------------------------
-- Séances : 4 semaines à partir des créneaux hebdomadaires
-- (passées = terminées, futures = planifiées)
-- ---------------------------------------------------------------------
with slots as (
    select
        g.id                    as group_id,
        g.teacher_id,
        sl.room_id,
        sl.weekday,
        sl.starts_at,
        sl.ends_at,
        date_trunc('week', current_date)::date as week_start
    from public.groups g
    join public.group_schedule_slots sl on sl.group_id = g.id
    join public.teachers t on t.id = g.teacher_id
    where t.full_name = 'M. Karim Lebbah'
),
occurrences as (
    select
        s.group_id,
        s.teacher_id,
        s.room_id,
        s.week_start + (s.weekday - 1) + (w * 7) as day,
        s.starts_at,
        s.ends_at
    from slots s
    cross join generate_series(0, 3) as w
)
insert into public.sessions (
    group_id, teacher_id, subject_id, room_id, starts_at, ends_at, status, topic
)
select
    o.group_id,
    o.teacher_id,
    (
        select ts.subject_id
          from public.teacher_subjects ts
         where ts.teacher_id = o.teacher_id
         order by ts.is_primary desc, ts.subject_id
         limit 1
    ),
    o.room_id,
    (o.day + o.starts_at) at time zone 'Africa/Algiers',
    (o.day + o.ends_at)   at time zone 'Africa/Algiers',
    case
        when (o.day + o.starts_at) at time zone 'Africa/Algiers' < now()
        then 'completed'::public.session_status
        else 'scheduled'::public.session_status
    end,
    'Séance de ' || coalesce(
        (
            select s.name
              from public.subjects s
              join public.teacher_subjects ts on ts.subject_id = s.id
             where ts.teacher_id = o.teacher_id
             order by ts.is_primary desc, s.name
             limit 1
        ),
        'cours'
    )
from occurrences o
on conflict (group_id, starts_at) do nothing;

-- ---------------------------------------------------------------------
-- Feuilles d'appel des séances terminées
-- (répartition déterministe : ~70 % présents)
-- ---------------------------------------------------------------------
insert into public.attendance (session_id, student_id, status, recorded_at)
select
    ss.id,
    st.id,
    case (abs(hashtext(st.matricule || ss.id::text)::bigint) % 10)
        when 0 then 'absent'::public.attendance_status
        when 1 then 'late'::public.attendance_status
        when 2 then 'justified'::public.attendance_status
        else 'present'::public.attendance_status
    end,
    ss.ends_at
from public.sessions ss
join public.enrollments e
  on e.group_id = ss.group_id
 and e.left_on is null
join public.students st on st.id = e.student_id
where ss.status = 'completed'
on conflict (session_id, student_id) do nothing;

-- ---------------------------------------------------------------------
-- Rotation de la vitrine : reconstitue les 5 derniers jours pour que
-- chaque enseignant éligible ait un historique équitable.
-- ---------------------------------------------------------------------
do $$
declare
    d date;
begin
    for d in
        select g::date
          from generate_series(current_date - 4, current_date, interval '1 day') as g
    loop
        perform * from public.rotate_featured_teachers(d, 4);
    end loop;
end;
$$;
