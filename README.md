# Duolingo KMP - App d'Apprentissage Multiplateforme (Kotlin / Compose Multiplatform)

Une application d'apprentissage style **Duolingo** développée en **Kotlin Multiplatform (KMP)** avec **Compose Multiplatform**, ciblant **Android**, **iOS (iPad / iPhone)** et **Windows Desktop**.

---

## 🎯 Domaines d'apprentissage inclus

1. **📐 Mathématiques Avancées** :
   - Algèbre linéaire, Vecteurs, Matrices, Produits scalaires & Valeurs propres.
   - Probabilités conditionnelles, Statistiques & Théorème de Bayes.

2. **💻 Informatique & Programmation C++** :
   - Structures de données (Arbres, Graphes, File/FIFO, Pile/LIFO).
   - Conception d'algorithmes et complexité temporelle $O(n \log n)$.
   - C++ moderne : Pointeurs, Déréférencement, Smart Pointers (`unique_ptr`, `shared_ptr`) & RAII.

3. **🇨🇦 Langues (Préparation TCF Canada & Anglais)** :
   - **TCF Canada** : Subjonctif présent, concordance des temps, compréhension écrite/orale & structures formelles.
   - **Anglais technique** : Vocabulaire de programmation et tournures professionnelles.

---

## 🤖 Ingestion & Transcription de Ressources par IA

L'application inclut un composant d'ingestion de ressources par IA (`AiContentPipeline`) permettant d'importer :
- 📄 **Fichiers PDF** (Cours de maths, manuels de C++, fiches TCF).
- ▶️ **Liens vidéo YouTube** (Tutoriels, explications de cours).

L'IA extrait le texte/transcription et génère dynamiquement des leçons avec exercices interactifs (Choix multiples, défis de code C++, calculs matriciels et reconstitution de phrases).

---

## 📱 Targets Multiplateformes

- **Windows Desktop** : `composeApp/src/desktopMain/kotlin/main.kt`
- **Android App** : `composeApp/src/androidMain/kotlin/com/duolingo/app/MainActivity.kt`
- **iOS / iPad OS** : Framework KMP exporté dans `composeApp/build.gradle.kts`

---

## 🚀 Publication sur GitHub

Le dépôt local a été initialisé dans ce dossier. Pour le publier sur votre compte GitHub :

1. Créez un nouveau dépôt sur [GitHub](https://github.com/new) nommé `duolingo-kmp-learning-app`.
2. Exécutez les commandes suivantes dans votre terminal :

```bash
cd "C:\Users\The_dr3amsigner\.gemini\antigravity\scratch\duolingo-kmp-learning-app"
git remote add origin https://github.com/VOTRE_NOM_DUTILISATEUR/duolingo-kmp-learning-app.git
git branch -M main
git push -u origin main
```
