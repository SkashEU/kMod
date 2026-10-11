# kMod

Just a playground project to see what's possible with Kotlin/Native.
The goal here is to build a clean, Kotlin idiomatic API for process memory manipulation on Windows by abstracting away the headache of raw C interop.

I will write more later


API currently looks like this. Might change later. Writing memory, error handling and customization will follow asap

````kotlin
    val process = WindowsProcess.byWindowName("AssaultCube") ?: return
    val module = process["ac_client.exe"] ?: return

    val healthPointer = module.deref(0x17E0A8, 0xEC)
    val health = module.read<IntVar, Int>(healthPointer) { it.value }
    println("Found health: $health")
````