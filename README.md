# Christina Bakirtzi Thesis Project
## Mobile Application for Chord Recognition & Substitution

### Abstract
  The scope of this thesis was the development of an Android mobile application for identifying and substituting the chords of a piece of musical piece. Chord sequences are the building blocks of tonal music and their analysis and processing is a very interesting issue in the field of music information retrieval.
  
  A deep learning model was adopted to recognize the chords corresponding to a music track, which was decided to run on an external server and communicate with the mobile application through internet requests. Moreover, the model’s outputs were further processed to extract two substitution proposals for each chord. The suggestions were based on statistic numbers collected from a set of chord progressions, which examined the frequency of occurrence of consecutive musical intervals between the chords of each sequence. Various ways of collecting and using these data were examined, to maximize the efficiency of the overall system in providing compatible and interesting replacement proposals.
  
  These music features were combined in a user-friendly mobile application compatible with the Android operating system, which provides the ability to record, save and play sounds through the device's microphone and speakers. The application communicates with a remote server so that the user receives information about the chords that correspond to the music track he chooses to record, as well as suggestions for substituting each of them. The final product was evaluated in terms of its individual and overall results, while its future extensions were also examined.


**Keywords:** Chord Recognition, Chord Substitution, Mobile Application, Deep Learning, Neural Networks, Music Information Retrieval, Chord, Note, Major, Minor, Audio Signal


### File Documentation:
* **APP:** Mobile Application Code
* **Bigram-Trigram Extraction:** Code used to extract Bigram/Trigram Dictionaries
* **Evaluation Scripts:** Scripts used to evaluate chord substitution system
* **VM Files:** Web app for chord recognition & chord substitution code

### Video:
![demo](https://user-images.githubusercontent.com/57402023/136834580-33929f65-b977-46eb-9238-2a637f6bbc30.mp4)
