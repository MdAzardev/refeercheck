package com.example.myapplication

data class ContainerData(
    val position: String,
    val containerNumber: String,
    val setTemp: String,
    val humidity: String,
    val vent: String,
    val pol: String,
    val pod: String,
    val opr: String,
    val reeferType: String = "",
)

data class Verification(
    val containerNumber: String,
    val actualTemp: String,
    val actualHumidity: String,
    val remark: String,
    val status: String,
    val alarmCode: String = "",
    val alarmDescription: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val latitude: Double? = null,
    val longitude: Double? = null,
    val photoPath: String? = null
)

data class UserProfile(
    val userId: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val company: String = "",
    val registrationDate: Long = System.currentTimeMillis(),
    val friends: List<String> = emptyList() // List of User IDs
)

