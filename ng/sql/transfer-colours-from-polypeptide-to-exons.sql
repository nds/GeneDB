
/* List colourless exons */
select exon.uniquename as exon
     , transcript.uniquename as transcript
     , polypeptide.uniquename as polypeptide
     , polypeptide_colour.value as colour
from feature exon
join feature_relationship exon_transcript on exon.feature_id = exon_transcript.subject_id
join feature transcript on transcript.feature_id = exon_transcript.object_id
join feature_relationship polypeptide_transcript on polypeptide_transcript.object_id = transcript.feature_id
join feature polypeptide on polypeptide_transcript.subject_id = polypeptide.feature_id
join featureprop polypeptide_colour on polypeptide.feature_id = polypeptide_colour.feature_id
join cvterm polypeptide_colour_term on polypeptide_colour.type_id = polypeptide_colour_term.cvterm_id
join cv polypeptide_colour_cv on polypeptide_colour_term.cv_id = polypeptide_colour_cv.cv_id
where exon.type_id in (
    select cvterm.cvterm_id
    from cvterm join cv using (cv_id)
    where cv.name = 'sequence'
    and cvterm.name in ('exon', 'pseudogenic_exon')
)
and transcript.type_id in (
    select cvterm.cvterm_id
    from cvterm join cv using (cv_id)
    where cv.name = 'sequence'
    and cvterm.name in ('pseudogenic_transcript', 'mRNA')
)
and polypeptide.type_id in (
    select cvterm.cvterm_id
    from cvterm join cv using (cv_id)
    where cv.name = 'sequence'
    and cvterm.name = 'polypeptide'
)
and polypeptide_colour_cv.name = 'genedb_misc'
and polypeptide_colour_term.name = 'colour'
and not exists (
    select *
    from featureprop
    where featureprop.feature_id = exon.feature_id
    and type_id in (
        select cvterm_id
        from cvterm join cv using (cv_id)
        where cv.name = 'genedb_misc'
        and cvterm.name = 'colour'
    )
)
;

begin;

create temporary table colourless_exons as
select exon.feature_id as exon_feature_id
     , transcript.feature_id as transcript_feature_id
     , polypeptide.feature_id as polypeptide_feature_id
     , polypeptide_colour.value as colour
from feature exon
join feature_relationship exon_transcript on exon.feature_id = exon_transcript.subject_id
join feature transcript on transcript.feature_id = exon_transcript.object_id
join feature_relationship polypeptide_transcript on polypeptide_transcript.object_id = transcript.feature_id
join feature polypeptide on polypeptide_transcript.subject_id = polypeptide.feature_id
join featureprop polypeptide_colour on polypeptide.feature_id = polypeptide_colour.feature_id
join cvterm polypeptide_colour_term on polypeptide_colour.type_id = polypeptide_colour_term.cvterm_id
join cv polypeptide_colour_cv on polypeptide_colour_term.cv_id = polypeptide_colour_cv.cv_id
where exon.type_id in (
    select cvterm.cvterm_id
    from cvterm join cv using (cv_id)
    where cv.name = 'sequence'
    and cvterm.name in ('exon', 'pseudogenic_exon')
)
and transcript.type_id in (
    select cvterm.cvterm_id
    from cvterm join cv using (cv_id)
    where cv.name = 'sequence'
    and cvterm.name in ('pseudogenic_transcript', 'mRNA')
)
and polypeptide.type_id in (
    select cvterm.cvterm_id
    from cvterm join cv using (cv_id)
    where cv.name = 'sequence'
    and cvterm.name = 'polypeptide'
)
and polypeptide_colour_cv.name = 'genedb_misc'
and polypeptide_colour_term.name = 'colour'
and not exists (
    select *
    from featureprop
    where featureprop.feature_id = exon.feature_id
    and type_id in (
        select cvterm_id
        from cvterm join cv using (cv_id)
        where cv.name = 'genedb_misc'
        and cvterm.name = 'colour'
    )
)
;

select count(*), count(distinct exon_feature_id)
from colourless_exons
;

/*
If the two numbers from the above are different, check for features with two colour properties:

select feature_id, fp1.featureprop_id, fp1.value
     , fp2.featureprop_id, fp2.value
from featureprop fp1
join featureprop fp2 using (feature_id, type_id)
where type_id in
(
        select cvterm_id
        from cvterm join cv using (cv_id)
        where cv.name = 'genedb_misc'
        and cvterm.name = 'colour'
)
and fp1.featureprop_id < fp2.featureprop_id
;

resolve them, and start again.
*/

insert into featureprop (feature_id, type_id, value) (
    select colourless_exons.exon_feature_id, cvterm.cvterm_id, colourless_exons.colour
    from colourless_exons
       , cvterm join cv using (cv_id)
    where cvterm.name = 'colour'
    and   cv.name     = 'genedb_misc'
);
