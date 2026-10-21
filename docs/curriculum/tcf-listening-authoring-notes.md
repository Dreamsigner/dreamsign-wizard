# Atelier original d’écoute — trois documents, six questions

Rédaction, synthèse et contrôle technique : 20 octobre 2026.

## Périmètre et sources

FEI définit la compréhension orale au TCF Canada sous forme de questions
sur des documents sonores :
https://www.france-education-international.fr/test/tcf-canada

La description indexée du format officiel a été consultée ; l’ouverture
directe du site renvoie ERR-BOT-403. Les trois documents de cet atelier sont
des créations originales, pas des sujets FEI ou une copie des banques
TV5MONDE/RFI. La progression locale va d’une annonce pratique à un message
professionnel puis à une opinion nuancée. Ce découpage n’est pas un programme
officiel et ne représente pas une préparation complète au TCF.

Les questions portent sur l’horaire, le public d’une consigne, une demande,
l’organisation d’une réunion, une opinion et les conditions qu’elle comporte.
Les notes et les indices ne révèlent pas les réponses des documents. La
transcription intégrale est affichée en correction. Les réécoutes sont permises
pendant cet entraînement ; aucun mode d’examen officiel ni score FEI/NCLC
n’est annoncé.

## Origine et format des enregistrements

Les textes exacts sont dans le catalogue JSON. Ils ont été synthétisés en
local avec `System.Speech.Synthesis.SpeechSynthesizer`, voix française
`Microsoft Hortense Desktop`, vitesse 0, volume 100. Aucun service distant,
enregistrement de personne ou média TV5MONDE/RFI n’a été utilisé. La mention
« voix de synthèse » est visible dans l’exercice. La voix n’est pas un
examinateur humain ; l’atelier ne reproduit pas la diversité des locuteurs
d’une vraie épreuve.

Reproduction Windows : `tools/build_listening_audio.ps1`. Ce script réutilise
une voix déjà installée, sans installation ni changement de configuration.
Les WAV sont intégrés au build et lus hors ligne avec les lecteurs natifs.
PCM signé, mono, 16 bits, 16 000 Hz.

| Document | Durée | SHA-256 |
| --- | --- | --- |
| `tcf_announcement.wav` | 24,07 s | `be28fe49ab5c33e74ef8190c5607b32347eaf296b58099eeb0c3d4e9d2386cf9` |
| `tcf_voicemail.wav` | 26,13 s | `db523fd22d519dc94f281f46bca9ab2579299b0b5a21cc0fa181f745a24b1a30` |
| `tcf_opinion.wav` | 28,13 s | `7d657687aa09f43fca3af2fa279ef4172b3ffdb3da042594d729c7d77d818950` |

## Validation et limites encore à lever

Le validateur contrôle présence, chemin borné, encodage, durée, signal non
silencieux et transcription. Le modèle n’accepte une réponse qu’après un
événement de fin de lecture pour le document courant ; arrêt, chargement
échoué et callback périmé ne valident pas l’écoute. Les deux questions d’un
même document peuvent réutiliser l’écoute achevée. Une nouvelle tentative
de la leçon réinitialise cet état.

L’intégration appelle l’arrêt sur navigation, destruction de l’écran et mise
en arrière-plan. Les tests métier et erreurs ne prouvent pas à eux seuls la
sortie sonore réelle : lecture native Windows, Android et iOS à vérifier.
Un mode lecture rend la transcription disponible avant la tentative. Il est
annoncé comme compréhension écrite, reste distinct d’un événement d’écoute
et les ateliers ainsi terminés sont identifiés dans le profil. Une révision
entièrement écoutée peut ensuite remplacer cette indication sans regagner de XP.
La production orale reste à réaliser.
