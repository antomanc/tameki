# R8 rules for the release build. Most of what Compose, Kotlin and Koin need is shipped in
# their own consumer rules; add app-specific keep rules here as features land.

# ML Kit discovers its components through registrars named in manifest meta-data and built by
# reflection; R8 stripped part of the GenAI Prompt graph (Generation.getClient() threw an NPE).
-keep class com.google.mlkit.** { *; }
-keep class com.google.android.gms.internal.mlkit_genai_prompt.** { *; }
# Document scanner (Play services) — keep scanning result / options used via reflection.
-keep class com.google.android.gms.internal.mlkit_vision_document_scanner.** { *; }
-keep class com.google.mlkit.vision.documentscanner.** { *; }
