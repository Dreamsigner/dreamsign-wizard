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

## ⚡ Comment tester et exécuter l'application ?

### Option 1 : Via Android Studio / IntelliJ IDEA (Recommandé)
1. Ouvrez **Android Studio** ou **IntelliJ IDEA**.
2. Choisissez **Open** et sélectionnez le dossier du projet : `C:\Users\The_dr3amsigner\.gemini\antigravity\scratch\duolingo-kmp-learning-app`.
3. Pour exécuter la version **Windows Desktop** :
   - Dans le panneau Gradle (à droite), développez `composeApp -> Tasks -> compose desktop -> desktopRun`.
   - Cliquez deux fois sur **desktopRun** pour lancer la fenêtre de l'application sur Windows.
4. Pour exécuter la version **Android** :
   - Sélectionnez la configuration `composeApp` et lancez un émulateur Android ou connectez votre smartphone/iPad.

### Option 2 : Via la ligne de commande (Terminal)
Exécutez la commande suivante dans le terminal à la racine du projet :
```bash
./gradlew :composeApp:desktopRun
```

---

## 📱 Structure Multiplateforme

- **Windows Desktop** : `composeApp/src/desktopMain/kotlin/main.kt`
- **Android App** : `composeApp/src/androidMain/kotlin/com/duolingo/app/MainActivity.kt`
- **Common Code (UI & Logique)** : `composeApp/src/commonMain/kotlin/com/duolingo/app`
