# Android WebView with Local Development Server

This project demonstrates a modern Android WebView implementation using Jetpack Compose with a local development server. It features bidirectional communication between Android and web content using Kotlin Flows.

## Project Structure

```
project/
├── app/                    # Android application
│   └── src/
│       └── main/
│           ├── java/      # Kotlin source files
│           └── res/       # Android resources
├── webserver/             # Local development server
│   ├── public/           
│   │   └── index.html    # Web content
│   ├── server.js         # Express server
│   └── package.json      # Node.js dependencies
└── README.md
```

## Prerequisites

- Android Studio Arctic Fox or newer
- Node.js 14.x or newer
- npm or yarn
- ngrok account (free tier works)
- Java 11 or newer

## Setup Instructions

### 1. Clone the Repository

```bash
git clone https://github.com/yourusername/android-webview-demo.git
cd android-webview-demo
```

### 2. Web Server Setup

```bash
# Navigate to web server directory
cd webserver

# Install dependencies
npm install

# Start the server
npm start
```

The server will start on `http://localhost:3000`

### 3. Start ngrok Tunnel

In a new terminal:

```bash
# If you haven't installed ngrok globally
npm install -g ngrok

# Start the tunnel
ngrok http 3000
```

Copy the HTTPS URL provided by ngrok (e.g., `https://abc123.ngrok.io`)

### 4. Configure Android Project

1. Open `app/src/main/java/com/example/webview/WebViewScreen.kt`
2. Update the WebView URL with your ngrok URL:

```kotlin
loadUrl("https://your-ngrok-url.ngrok.io")
```

### 5. Run the Android App

1. Open the project in Android Studio
2. Connect an Android device or start an emulator
3. Click Run or press Shift + F10

## Development Workflow

1. Start the local server (`npm start` in webserver directory)
2. Start ngrok (`ngrok http 3000`)
3. Update the WebView URL if needed
4. Run the Android app

Changes to web content in the `public` folder will be immediately available on refresh.

## Communication API

### Web to Android

```javascript
// Send message to Android
Android.sendMessageToAndroid('Hello from web!')

// Show toast
Android.showToast('Message')
```

### Android to Web

```kotlin
// In your Composable
val scope = rememberCoroutineScope()
scope.launch {
    webViewState.sendMessageToWeb("Hello from Android!")
}

// Receive messages
LaunchedEffect(Unit) {
    webViewState.webMessages.collect { message ->
        // Handle message
    }
}
```

## Documentation

- Jetpack Compose documentation
- Android WebView documentation
- Express.js
- ngrok documentation

## Contact

Your Name - [@yourusername](https://twitter.com/yourusername)
Project Link: [https://github.com/yourusername/android-webview-demo](https://github.com/yourusername/android-webview-demo)
