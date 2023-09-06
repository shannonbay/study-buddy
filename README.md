# study-buddy
A repeat reading app which breaks a text into bite-size chunks for more focused listening.  It also pauses and resumes play-back by detecting background noise levels via the microphone.   This gives the listener time to verbally repeat or paraphrase the content, and also makes disruptions less of a hassle.  Finally, it transcribes your own speech. 

# Code style and tips
* Use Android Studio
* Prefer to determine state of resources directly rather than inferring through ad-hoc state models
* Use Log.d as default log level - only use Log.e or Log.i if end-user would need it for troubleshooting
