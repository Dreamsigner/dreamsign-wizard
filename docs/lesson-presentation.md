# Présentation des leçons

Le lecteur natif distingue le texte pédagogique, les expressions mathématiques,
le code et les ressources. Le même composant de lecture est utilisé dans les
cours, les notes de séance et l'aide; les corrections et indices emploient
également ce format.

## Principes éditoriaux

- Donner à chaque section un titre décrivant son contenu, sans titre décoratif.
- Limiter chaque paragraphe à une idée principale et séparer les étapes d'un
  raisonnement. Un titre ne remplace pas une explication.
- Mettre en gras les notions qui aident à comprendre la section, avec parcimonie.
  La couleur d'accent identifie les repères et les actions; elle ne porte jamais
  seule une distinction pédagogique.
- Présenter les exemples résolus près de la règle étudiée, puis proposer une
  pratique associée. Distinguer exemple, résultat et précaution.
- Conserver le code littéral en chasse fixe. Préciser le langage et son contexte,
  notamment Bash par rapport à PowerShell. Ne pas transformer une variable du
  code en expression mathématique par simple ressemblance.
- Utiliser LaTeX explicite pour les expressions composées. Les matrices et les
  longues égalités occupent un bloc séparé; une expression courte reste dans la
  phrase. Un indice et un exposant doivent conserver leur sens distinct.

## Format d'édition

`learningNotes` reste un champ texte compatible avec les données existantes.
Son sous-ensemble Markdown comprend les paragraphes séparés par une ligne vide,
les titres `##` et `###`, les listes `-`, les encadrés `>`, le gras `**...**`,
le code entre accents graves et les blocs de code délimités par trois accents
graves. Les formules utilisent `$...$` et `$$...$$`.

Le lecteur n'exécute pas de HTML et ne charge pas d'images distantes à partir
des notes. Un bloc de code n'est ni normalisé comme une formule ni exécuté.
Les titres sont exposés comme tels dans la sémantique Compose.

Le texte courant utilise une taille de 17 sp, un interligne de 28 sp et une
largeur maximale de 680 dp. Le contenu se replie avec la fenêtre. Les blocs de
code et les grandes formules peuvent défiler horizontalement, indépendamment
du texte courant. Ces valeurs sont des choix d'interface, pas une garantie
d'un nombre fixe de caractères par ligne sur tous les écrans.

## Couverture et contrôles

Le lecteur s'applique à tout le catalogue. Les neuf leçons Missing Semester et
les trois premières leçons MIT 18.06SC ont reçu une restructuration éditoriale
explicite avec sections et exemples; cette révision ne vaut pas relecture
éditoriale approfondie des 391 séances. Une passe de ponctuation couvre aussi
les autres notes : les séparateurs de prose trop fréquents deviennent des
phrases courtes, sans modifier les données, le code ou les citations. Les
identifiants de commande et de fonction balisés sont présentés en chasse fixe
sur un fond distinct. Les autres notes bénéficient de la largeur de lecture
et du rendu typographique communs sans ajout automatique de titres artificiels.

Les tests vérifient le découpage, la préservation du code, les indices et
exposants, le placement des formules dans un seul paragraphe natif, et les
écrans de lecture aux largeurs 1100 et 500 pixels. Leurs captures sont produites
sur des surfaces identiques à celles de l'application, sans automatisation du
bureau. Voir aussi [le rendu mathématique](math-typesetting.md).

## Références de conception

- [W3C WAI — Use a Clear and Understandable Page Structure](https://www.w3.org/WAI/WCAG2/supplemental/patterns/o2p03-page-structure/) : sections logiques, hiérarchie et repères visuels.
- [W3C WAI — Page Structure Tutorial](https://www.w3.org/WAI/tutorials/page-structure/) : titres et navigation dans le contenu.
- [IES — Organizing Instruction and Study to Improve Student Learning](https://ies.ed.gov/ncee/wwc/PracticeGuide/1) : alternance d'exemples résolus et de problèmes, articulation des représentations concrètes et abstraites.

Ces références motivent les choix de présentation; elles ne constituent pas
une mesure de l'efficacité pédagogique propre à cette application.
