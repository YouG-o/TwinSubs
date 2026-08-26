# TwinSubs

TwinSubs is a user-friendly GUI application designed to merge two subtitle tracks into a single, stylized bilingual ASS subtitle track.

Designed primarily for language learners, TwinSubs helps you master new languages by allowing you to watch videos with both the original language and your native translation displayed simultaneously. With advanced styling options, you can emphasize your target language while using the translation as a subtle secondary reference, making it the perfect tool for immersive language study.

**Warning: TwinSubs is currently in active development. A functional portable release (Windows/macOS/Linux) is not yet available.**

## Key Features

- **Bilingual Fusion:** Merge two distinct subtitle tracks into one.
- **Advanced Styling:** Generate ASS (Advanced SubStation Alpha) files with customizable fonts, colors, and positioning for each language.
- **Media Support:** Native support for MKV and MP4 files.
- **Easy Processing:** Drag-and-drop interface with batch processing capabilities for folders.
- **Flexible Output:** Generate external `.ass` files or embed them directly into MKV containers via remuxing.
- **User-Friendly:** No command-line knowledge required.

## How to Build (For Developers)

TwinSubs is built with Java 21 and JavaFX.

### Prerequisites
1. **Java 21+** (JDK 21) installed on your machine.
2. **FFmpeg & FFprobe**: Must be installed and available in your system `PATH`.
   - macOS: `brew install ffmpeg`
   - Linux: `sudo apt install ffmpeg`
   - Windows: Install via [ffmpeg.org](https://ffmpeg.org/download.html) or `winget install FFmpeg`.

### Building the Project
Clone the repository and run the following Maven command:

```bash
mvn clean javafx:run
```

## Download & Releases

TwinSubs is currently in its V0 stage. 

**Coming soon:** Future releases will provide **portable builds** (Windows, macOS, Linux). These releases will bundle FFmpeg and the JRE/JDK, ensuring a 100% "plug-and-play" experience for end-users, requiring no external installation.

## The V0 Pipeline
TwinSubs V0 focuses on a robust temporal matching algorithm:
1. **Extraction:** Uses FFmpeg to extract subtitle streams.
2. **Parsing:** Converts SRT streams into a unified domain model.
3. **Matching:** Aligns tracks based on temporal overlap.
4. **Rendering:** Generates a bilingual ASS file with custom-defined layout and styles.

## License

TwinSubs is open source and 100% free to use.