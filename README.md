# AeroPower

AeroPower is a modern Android application designed to help users practice guided breathing exercises for stress reduction, relaxation, and mindfulness. The app features customizable breathing patterns, visual feedback, and habit tracking to encourage regular practice.

![AeroPower Logo](app/src/main/res/drawable-v24/ic_launcher_foreground.xml)

## Features

### Core Breathing Experience
- **Visual Guidance**: Interactive breathing circle that expands and contracts to guide your breathing rhythm
- **Multiple Breathing Techniques**:
  - Box Breathing (4-4-4-4)
  - 4-7-8 Technique (4-7-8-0)
  - Calming Breath (6-2-7-0)
  - Custom pattern with adjustable durations
- **Customizable Sessions**: Adjust the number of breathing cycles (1-10)
- **Progress Tracking**: Visual ring shows progress through your breathing session

### Sensory Feedback
- **Sound Effects**: Optional audio cues for phase transitions
- **Haptic Feedback**: Gentle vibrations to guide your breathing without looking at the screen
- **Soothing Background Music**: Optional ambient sounds to enhance relaxation

### Habit Building
- **Daily Habit Tracker**: Calendar view showing your practice history
- **Streak Counter**: Track consecutive days of practice
- **Daily Reminders**: Set customizable notifications to maintain your practice routine

### User Experience
- **Portrait & Landscape Support**: Optimized layouts for both orientations
- **Accessibility Features**: TalkBack support and detailed spoken instructions
- **Dark Theme**: Eye-friendly dark interface ideal for evening use
- **Battery Optimization**: Smart power management for extended practice sessions

## Technical Overview

AeroPower is built with modern Android development practices:

- **Architecture**: MVVM (Model-View-ViewModel) with LiveData
- **UI Components**: Material Design components and custom views
- **Data Persistence**: Room database for tracking practice history
- **Animations**: Custom animations for the breathing circle and UI transitions

## Screenshots

(Screenshots would be placed here)

## Getting Started

### Prerequisites
- Android Studio Electric Eel (2022.1.1) or newer
- Minimum SDK: Android 8.0 (API level 26)
- Target SDK: Android 14 (API level 34)

### Building the Project
1. Clone the repository
2. Open the project in Android Studio
3. Sync Gradle files
4. Build the project

## Implementation Details

### Key Components

- **BreathingViewModel**: Core business logic for the breathing exercises
- **HALCircleView**: Custom view for the interactive breathing circle
- **BreathingUIController**: Manages UI state and user interactions
- **Room Database**: Stores session history for habit tracking

### Project Structure

- **data/**: Database and data models
- **model/**: Core domain models
- **ui/**: UI components and custom views
- **utils/**: Utility classes for vibration, sound, etc.
- **viewmodel/**: ViewModels and state management

## Customization

### Breathing Patterns

Custom breathing patterns can be created by adjusting:
- Inhale duration (1-10 seconds)
- Hold after inhale (0-10 seconds)
- Exhale duration (1-10 seconds)
- Hold after exhale (0-10 seconds)

### Appearance

The app uses a soothing color palette with cyan and green hues, designed to promote relaxation.

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Acknowledgments

- HAL design inspired by minimalist UI principles
- Sound effects designed to enhance mindfulness practice
