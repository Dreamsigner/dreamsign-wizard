# Périmètre actif de livraison

## Décision utilisateur — 20 octobre 2026

L'utilisateur demande de continuer uniquement Windows si l'avancement estimé
du périmètre complet est inférieur à 50 %. L'estimation donnée est d'environ
40 % : moteur, interface et tests avancés, couverture pédagogique intégrale
encore loin d'être atteinte. C'est une estimation de travail, pas un pourcentage
calculé de séances sources dont tous les décomptes seraient connus.

Le périmètre actif est donc **Windows uniquement**. Le code Android/iOS déjà
présent est conservé, sans développement ni build de ces plateformes dans le
workflow actif. Les résultats mobiles historiques restent des traces datées,
pas des conditions de la livraison Windows. Aucune signature Apple n'est un
prérequis du périmètre actif.

Le socle Kotlin/Compose natif, la charte, la progression locale persistante,
l'absence de génération IA et les exigences de contenu sourcé restent valables.
Le catalogue ne doit pas être déclaré intégral tant que les cours retenus ne
sont pas réellement rédigés et contrôlés. Les brouillons incomplets restent
hors catalogue. Aucun Computer Use et aucune automatisation de reprise pour
ce travail manuel.

Le workflow Build exécute les vérifications de sources, les tests desktop, la
construction Windows et le diagnostic du véritable lanceur/ressources intégrés.
Les preuves figurent dans `EXECUTION-LOG.md`, `automated-testing.md` et les
artefacts du commit correspondant.
