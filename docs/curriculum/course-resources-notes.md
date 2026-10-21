# Références officielles des cours

Mis à jour le 20 octobre 2026. Le catalogue `curriculum/course-resources.json` décrit les ressources externes consultables depuis les 15 cours publiés. Il ne contient aucun fichier source téléchargé. Les liens de séance proviennent des `sourceUrls` déjà attachés aux leçons canoniques; les liens vidéo ont été relevés dans les pages officielles correspondantes. Une ressource de séance est également rattachée à chacun des exercices de cette séance.

## Contrat Kotlin

`CourseResources.forCourse(courseId)` renvoie les ressources générales d’un cours. `forSession(courseId, sessionNumber)` renvoie les ressources précises d’une séance. `forExercise(exerciseId)` résout l’exercice vers sa séance puis renvoie la même liste. Les ressources ne sont donc pas copiées en mémoire dans une liste par exercice. L’identifiant de cours est le `courseId` du catalogue; l’identifiant d’exercice est l’identifiant stable de l’exercice canonique.

`CourseResource` expose `id`, `title`, `url`, `kind`, `coverage`, `language`, `licenseStatus`, `sourceUrl`, `licenseEvidenceUrl` et `notes`. `kind` distingue notamment l’index du cours, une collection, une section, un PDF, une vidéo ou un téléchargement. `coverage` distingue l’index de cours, les supports d’une séance, un manuel complet et un document partiel. Les liens vidéo d’origine dans les données de leçons ne sont pas retirés ni remplacés.

## Provenance et portée vérifiées

- **MIT OpenCourseWare** : les pages d’index `download/` existent pour les sept cours OCW du catalogue. Les liens précis des leçons sont extraits des leçons, et ne sont pas présentés comme un manuel complet. La page des conditions OCW indique CC BY-NC-SA 4.0 pour le site tout en signalant des exceptions possibles pour les éléments individuels. Le catalogue ne redistribue rien et conserve ce caveat. Pour 6.042J, les notes directes sont marquées `partial_textbook`: le PDF consulté fait 557 pages, les notes sont copyright 2010 / « all rights reserved », l’index OCW précise qu’elles sont utilisées avec permission et le chapitre 8 n’est pas disponible dans cette édition. Aucun droit de réhébergement n’est déduit.
- **forall x: Calgary** : le lien direct mène au PDF Fall 2025 consulté (437 pages) et la page du manuel indique CC BY 4.0. Le PDF reste externe et n’est pas recopié. La page du livre mentionne aussi des extraits utilisés avec permission; la licence générale du manuel ne doit pas être étendue à ces éléments tiers.
- **The Missing Semester** : la page officielle de licence annonce CC BY-NC-SA 4.0. Le catalogue ne copie pas les notes ou médias.
- **Algorithmic Game Theory (Fall 2013)** : les notes Tim Roughgarden sont référencées par les liens officiels précis déjà portés par chaque séance. Les notes consultées indiquent un usage personnel uniquement; les liens restent externes.
- **Learn Prolog Now!** : la page du livre en ligne est référencée. Aucune licence de redistribution ou version PDF complète autorisée n’a été vérifiée.
- **TCF Canada** : seuls les liens d’information officiels de France Éducation international sont indiqués. Aucune licence de redistribution n’a été vérifiée et aucun PDF de manuel n’est revendiqué.

## Contrôle des liens et limites

Le 20 octobre 2026, 303 URL PDF distinctes déjà liées par les leçons ont répondu `200` avec le type MIME `application/pdf`. Les pages `download/` MIT consultées ont répondu `200`. Le script `tools/validate_course_resources.py` vérifie localement le rattachement des séances aux cours publiés, les URL HTTPS, les métadonnées, la cohérence avec les sources de leçon, les hôtes vidéo et l’unicité des identifiants. Il ne vérifie pas chaque lien sur le réseau à chaque exécution, ne prouve pas les droits d’utilisation et ne garantit pas la disponibilité future des sites.

Le repérage vidéo conserve les URL YouTube spécifiques et les MP4 explicitement liés par les pages officielles. Il peut suivre un lien vidéo explicite du même cours sur un seul niveau, notamment les pages MIT « Watch the video lecture » et « Topic Videos ». Les liens de chaînes ne sont pas des liens vidéo et sont exclus de cette catégorie. `sourceUrl` conserve la provenance de la découverte; `url` ouvre la vidéo. L'interface distingue les liens YouTube des fichiers MP4, et les références générales des liens vidéo. Elle ne fusionne pas arbitrairement des clips distincts simplement parce qu'ils proviennent de la même page.

La collecte du 21 octobre 2026 comprend 383 entrées vidéo pour 333 URL distinctes. Les 70 liens de MIT 18.06SC couvrent 35 séances sur 40; les 90 liens de 6.004 couvrent 27 séances sur 28, avec plusieurs clips possibles. Les révisions, examens ou cours sans vidéo ne reçoivent pas de lien inventé. La disponibilité et la lecture de chaque média externe ne sont pas garanties : une page inaccessible ou un lecteur rendu par script peut ne pas fournir de lien direct. Le lien de séance demeure une référence générale distincte. Le catalogue est un index de références, pas une déclaration que chaque cours possède un seul document complet.

## Sources de licence

- MIT OCW, conditions et exceptions : https://ocw.mit.edu/pages/privacy-and-terms-of-use/
- Notes de lecture OCW 6.042J et limite de chapitre : https://ocw.mit.edu/courses/6-042j-mathematics-for-computer-science-fall-2010/pages/readings/
- forall x: Calgary, manuel et licence : https://forallx.openlogicproject.org/html/Chx1.html
- The Missing Semester, licence : https://missing.csail.mit.edu/license/
- Notes Algorithmic Game Theory, exemple de mention d’utilisation personnelle : https://timroughgarden.org/f13/l/l1.pdf
- TCF Canada, page officielle FEI : https://www.france-education-international.fr/test/tcf-canada
