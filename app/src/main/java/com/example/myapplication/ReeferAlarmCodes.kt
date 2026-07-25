package com.example.myapplication

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import java.io.File

data class AlarmComponent(
    @SerializedName("component") val component: String = "",
    @SerializedName("troubleshooting") val troubleshooting: String = "",
    @SerializedName("correctiveaction") val correctiveAction: String = "",
)

data class AlarmCode(
    @SerializedName("alarmcode") val code: String = "",
    @SerializedName("alarmdescription") val description: String = "",
    @SerializedName("cause") val cause: String = "",
    @SerializedName("brand") val brand: String = "Carrier",
    @SerializedName("model") val model: String = "",
    @SerializedName("correctiveAction") val correctiveAction: String = "",
    @SerializedName("controllerAction") val controllerAction: String = "",
    @SerializedName("components") val components: List<AlarmComponent> = emptyList(),
)

data class AlarmDatabaseRoot(
    val alarms: List<AlarmCode>
)

object AlarmCodeProvider {
    val alarmCodes = mutableStateListOf<AlarmCode>()

    private fun getInternalFile(context: Context): File {
        return File(context.filesDir, "carrier alarm_database.json")
    }

    fun loadData(context: Context) {
        alarmCodes.clear()
        
        val assetFiles = listOf(
            Triple("carrier alarm_database.json", "Carrier", ""),
            Triple("daikin lx10e alarm_database.json", "Daikin", "LX10E"),
            Triple("daikin lx10f alarm_database.json", "Daikin", "LX10F"),
            Triple("star cool cim 6 alarm_database.json", "StarCool", "CIM 6"),
            Triple("star cool cim5 alarm_database.json", "StarCool", "CIM 5"),
            Triple("thermoking alarm_database.json", "Thermo King", "")
        )

        assetFiles.forEach { (fileName, brand, model) ->
            try {
                val json = context.assets.open(fileName).bufferedReader().use { it.readText() }
                mergeFromJson(json, brand, model)
            } catch (e: Exception) { e.printStackTrace() }
        }

        // 2. Merge from Internal Storage (User additions/updates)
        val internalFile = getInternalFile(context)
        if (internalFile.exists()) {
            try {
                val json = internalFile.readText()
                mergeFromJson(json)
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    private fun mergeFromJson(json: String, assetBrand: String? = null, assetModel: String? = null) {
        val gson = Gson()
        val incomingList = mutableListOf<AlarmCode>()

        try {
            var i = 0
            while (i < json.length) {
                val start = json.indexOfAny(charArrayOf('{', '['), i)
                if (start == -1) break

                val opener = json[start]
                val closer = if (opener == '{') '}' else ']'
                var balance = 0
                var end = -1
                var inString = false
                var escaped = false
                
                for (j in start until json.length) {
                    val char = json[j]
                    if (escaped) {
                        escaped = false
                        continue
                    }
                    if (char == '\\') {
                        escaped = true
                        continue
                    }
                    if (char == '"') {
                        inString = !inString
                        continue
                    }
                    
                    if (!inString) {
                        if (char == opener) balance++
                        else if (char == closer) balance--
                        
                        if (balance == 0) {
                            end = j
                            break
                        }
                    }
                }

                if (end != -1) {
                    val content = json.substring(start, end + 1)
                    try {
                        if (opener == '{') {
                            val map = try { gson.fromJson<Map<String, Any>>(content, object : TypeToken<Map<String, Any>>() {}.type) } catch (_: Exception) { null }
                            if (map != null) {
                                if (map.containsKey("alarms")) {
                                    val alarmsRaw = map["alarms"] as? List<Map<String, Any>>
                                    alarmsRaw?.forEach { itemMap ->
                                        parseAlarmFromMap(itemMap, assetBrand, assetModel)?.let { alarm ->
                                            val componentsRaw = itemMap["components"] as? List<Map<String, Any>>
                                            val componentsList = componentsRaw?.map { compMap ->
                                                AlarmComponent(
                                                    component = compMap["component"]?.toString() ?: "",
                                                    troubleshooting = compMap["troubleshooting"]?.toString() ?: "",
                                                    correctiveAction = (compMap["correctiveaction"] ?: compMap["correctiveAction"] ?: compMap["corrective Action"])?.toString() ?: ""
                                                )
                                            } ?: emptyList()
                                            
                                            incomingList.add(alarm.copy(components = componentsList))
                                        }
                                    }
                                } else {
                                    parseAlarmFromMap(map, assetBrand, assetModel)?.let { incomingList.add(it) }
                                }
                            }
                        } else {
                            val listType = object : TypeToken<List<Map<String, Any>>>() {}.type
                            val rawList = try { gson.fromJson<List<Map<String, Any>>>(content, listType) } catch (_: Exception) { null }
                            rawList?.forEach { itemMap ->
                                parseAlarmFromMap(itemMap, assetBrand, assetModel)?.let { incomingList.add(it) }
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    i = end + 1
                } else {
                    i = start + 1
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        incomingList.forEach { item ->
            val index = alarmCodes.indexOfFirst { 
                it.code.equals(item.code, ignoreCase = true) && 
                it.brand.equals(item.brand, ignoreCase = true) &&
                it.model.equals(item.model, ignoreCase = true)
            }
            if (index != -1) {
                alarmCodes[index] = item
            } else {
                alarmCodes.add(item)
            }
        }
    }

    private fun parseAlarmFromMap(map: Map<String, Any>, assetBrand: String?, assetModel: String?): AlarmCode? {
        val code = (map["alarmcode"] ?: map["alarm no."] ?: map["alarm no"] ?: map["Code"] ?: map["code"] ?: map["alarm code"] ?: map["alarm_code"])?.toString()?.trim()?.removeSuffix(".0") ?: ""
        if (code.isEmpty() || code == "null") return null

        val description = (map["alarmdescription"] ?: map["alarm description"] ?: map["Name"] ?: map["description"] ?: map["alarm_content"])?.toString()?.takeIf { it != "nan" } ?: ""
        val cause = (map["cause"] ?: map["Details"] ?: map["Indication"] ?: map["Description"] ?: map["possible_cause"])?.toString()?.takeIf { it != "nan" } ?: ""
        val corrective = (map["Corrective Action"] ?: map["Trouble shooting"] ?: map["troubleshooting"] ?: map["troubleshootin"] ?: map["correctiveAction"] ?: map["correctiveaction"])?.toString()?.takeIf { it != "nan" } ?: ""
        val action = (map["controller action"] ?: map["controller_action"] ?: map["Controller action"] ?: map["Action"] ?: map["controllerAction"])?.toString()?.takeIf { it != "nan" } ?: ""
        val brand = (map["brand"]?.toString() ?: assetBrand ?: "Carrier").trim()
        val model = (map["model"]?.toString() ?: assetModel ?: "").trim()

        return AlarmCode(
            code = code,
            description = description,
            cause = cause,
            brand = brand,
            model = model,
            correctiveAction = corrective,
            controllerAction = action
        )
    }

    fun saveData(context: Context) {
        try {
            val json = Gson().toJson(AlarmDatabaseRoot(alarmCodes.toList()))
            getInternalFile(context).writeText(json)
        } catch (e: Exception) { e.printStackTrace() }
    }
}
