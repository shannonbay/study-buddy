# study-buddy
An audio streaming companion app for touchless playback control, perfect for listening on the go... and the sight-impaired
It works by detecting any speech at all using VAD
When it hears speech, it pauses until speech ends plus a configurable timeout, then it rewinds by 5 seconds and resumes

## Code style and tips
* Use Android Studio
* Prefer to determine state of resources directly rather than inferring through ad-hoc state models
* Use Log.d as default log level - only use Log.e or Log.i if end-user would need it for troubleshooting

## Features
* Pause, resumes and rewind play-back by detecting speech.
* Compatible with Spotify, Subsplash and many other media players
* Works offline - except rewind for Spotify
* Configurable resume delay

## Future features
* Configurable rewind
* Rewind more or less depending on pause duration
* A repeat reading app which breaks a text into bite-size chunks for more focused listening. - POC done. This gives the listener time to verbally repeat or paraphrase the content, and also makes disruptions less of a hassle.
* Record your own speech and transcribe for an end-of-day review/journal update.
* Natural language Date/time search of transcribed speech/journal entries
* Text-oriented Chronological list view of date/time entries, with seek to time (eg now) and various natural language schedule queries like 'next event' or 'what's happening on Tuesday?'

