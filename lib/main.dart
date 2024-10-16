import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

class PoseDetection {
  static const platform = MethodChannel('com.example.powerlift_analysis/pose');

  Future<void> startPoseDetection() async {
    try {
      final String result = await platform.invokeMethod('startPoseDetection');
      print(result); // This will print "Pose detection started"
    } on PlatformException catch (e) {
      print("Failed to start pose detection: '${e.message}'.");
    }
  }
}

void main() {
  runApp(const MainApp());
}

class MainApp extends StatelessWidget {
  const MainApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: const Text('Powerlift Analysis'),
        ),
        body: Center(
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: <Widget>[
              ElevatedButton(
                onPressed: () {
                  PoseDetection().startPoseDetection(); // Call the pose detection method
                  print("Squat button pressed");
                },
                child: const Text('Squat'),
              ),
              const SizedBox(height: 20),
              ElevatedButton(
                onPressed: () {
                  print("Deadlift button pressed");
                },
                child: const Text('Deadlift'),
              ),
              const SizedBox(height: 20),
              ElevatedButton(
                onPressed: () {
                  print("Bench button pressed");
                },
                child: const Text('Bench'),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
