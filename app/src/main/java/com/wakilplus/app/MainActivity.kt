package com.wakilplus.app

import android.os.Bundle
import android.content.Context
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WakilPlusApp()
        }
    }
}

data class AppSection(
    val title: String,
    val icon: String,
    val color: Color
)

data class Beneficiary(
    val id: Long,
    val name: String,
    val address: String,
    val phone: String,
    val members: String,
    val cardNumber: String,
    val nfc: String
)

private const val STORAGE_KEY = "wakilplus_beneficiaries"

fun loadBeneficiaries(context: Context): List<Beneficiary> {
    return try {
        val json = context.getSharedPreferences(
            "WakilPlusData", Context.MODE_PRIVATE
        ).getString(STORAGE_KEY, "[]") ?: "[]"

        val array = JSONArray(json)
        val result = mutableListOf<Beneficiary>()

        for (i in 0 until array.length()) {
            val item = array.getJSONObject(i)
            result.add(
                Beneficiary(
                    id = item.optLong("id"),
                    name = item.optString("name"),
                    address = item.optString("address"),
                    phone = item.optString("phone"),
                    members = item.optString("members"),
                    cardNumber = item.optString("cardNumber"),
                    nfc = item.optString("nfc")
                )
            )
        }
        result
    } catch (_: Exception) {
        emptyList()
    }
}

fun saveBeneficiaries(
    context: Context,
    beneficiaries: List<Beneficiary>
) {
    val array = JSONArray()

    beneficiaries.forEach { person ->
        val item = JSONObject()
        item.put("id", person.id)
        item.put("name", person.name)
        item.put("address", person.address)
        item.put("phone", person.phone)
        item.put("members", person.members)
        item.put("cardNumber", person.cardNumber)
        item.put("nfc", person.nfc)
        array.put(item)
    }

    context.getSharedPreferences(
        "WakilPlusData", Context.MODE_PRIVATE
    ).edit().putString(STORAGE_KEY, array.toString()).apply()
}

@Composable
fun WakilPlusApp() {
    val context = LocalContext.current

    val beneficiaries = remember {
        mutableStateListOf<Beneficiary>().apply {
            addAll(loadBeneficiaries(context))
        }
    }

    var selectedSection by remember { mutableStateOf("") }
    var search by remember { mutableStateOf("") }
    var showForm by remember { mutableStateOf(false) }
    var editingPerson by remember { mutableStateOf<Beneficiary?>(null) }

    val sections = listOf(
        AppSection("المستفيدون", "👥", Color(0xFF168B68)),
        AppSection("الحصة الغذائية", "🛒", Color(0xFF2878C7)),
        AppSection("مادة الطحين", "🌾", Color(0xFFD18B26)),
        AppSection("الرعاية الاجتماعية", "🤝", Color(0xFF8656B8)),
        AppSection("التسليم", "📦", Color(0xFF168B68)),
        AppSection("الديون والحسابات", "💰", Color(0xFF2878C7)),
        AppSection("كشف المتغيرات", "📊", Color(0xFFD18B26)),
        AppSection("التقارير", "📄", Color(0xFF8656B8)),
        AppSection("قارئ PDF", "📕", Color(0xFF168B68)),
        AppSection("سجل التحركات", "📋", Color(0xFF2878C7)),
        AppSection("حافظة الملفات", "🗂️", Color(0xFFD18B26)),
        AppSection("الإعدادات", "⚙️", Color(0xFF8656B8))
    )

    MaterialTheme {
        CompositionLocalProvider(
            LocalLayoutDirection provides LayoutDirection.Rtl
        ) {
            Scaffold(
                containerColor = Color(0xFFF3F6F8),
                topBar = {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF126B53))
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "وكيل بلس",
                            color = Color.White,
                            fontSize = 25.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.weight(1f))
                        Text(
                            "نظام الوكيل التمويني",
                            color = Color.White,
                            fontSize = 12.sp
                        )
                    }
                }
            ) { padding ->

                if (selectedSection == "المستفيدون") {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .padding(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "سجل المستفيدين",
                                fontSize = 21.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(
                                onClick = { selectedSection = "" }
                            ) {
                                Text("الرئيسية")
                            }
                        }

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFF126B53)
                            )
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Text(
                                    "عدد المستفيدين",
                                    color = Color.White
                                )
                                Text(
                                    beneficiaries.size.toString(),
                                    color = Color.White,
                                    fontSize = 30.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        OutlinedTextField(
                            value = search,
                            onValueChange = { search = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("ابحث بالاسم أو رقم البطاقة") },
                            singleLine = true
                        )

                        Spacer(Modifier.height(10.dp))

                        Button(
                            onClick = {
                                editingPerson = null
                                showForm = true
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF126B53)
                            )
                        ) {
                            Text("＋ إضافة مستفيد جديد")
                        }

                        Spacer(Modifier.height(10.dp))

                        val filtered = beneficiaries.filter {
                            it.name.contains(search, true) ||
                            it.cardNumber.contains(search, true) ||
                            it.phone.contains(search, true)
                        }

                        if (filtered.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(20.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    if (beneficiaries.isEmpty())
                                        "لا يوجد مستفيدون حالياً"
                                    else
                                        "لا توجد نتائج للبحث"
                                )
                            }
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(filtered, key = { it.id }) { person ->
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(
                                            containerColor = Color.White
                                        )
                                    ) {
                                        Column(
                                            Modifier.padding(14.dp)
                                        ) {
                                            Text(
                                                person.name,
                                                fontSize = 18.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF126B53)
                                            )
                                            Spacer(Modifier.height(6.dp))
                                            Text("العنوان: ${person.address}")
                                            Text("الهاتف: ${person.phone}")
                                            Text("عدد أفراد الأسرة: ${person.members}")
                                            Text("رقم البطاقة: ${person.cardNumber}")
                                            if (person.nfc.isNotBlank()) {
                                                Text("رقم NFC: ${person.nfc}")
                                            }

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.End
                                            ) {
                                                TextButton(
                                                    onClick = {
                                                        editingPerson = person
                                                        showForm = true
                                                    }
                                                ) {
                                                    Text("تعديل")
                                                }

                                                TextButton(
                                                    onClick = {
                                                        beneficiaries.remove(person)
                                                        saveBeneficiaries(
                                                            context,
                                                            beneficiaries
                                                        )
                                                    }
                                                ) {
                                                    Text(
                                                        "حذف",
                                                        color = Color.Red
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .padding(16.dp)
                    ) {
                        if (selectedSection.isNotEmpty()) {
                            TextButton(
                                onClick = { selectedSection = "" }
                            ) {
                                Text("← العودة للرئيسية")
                            }
                            Text(
                                selectedSection,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(12.dp))
                            Text("سيتم تجهيز هذا القسم في المراحل القادمة.")
                        } else {
                            Text(
                                "لوحة التحكم الرئيسية",
                                fontSize = 21.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF183C32)
                            )

                            Spacer(Modifier.height(14.dp))

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFF126B53)
                                ),
                                shape = RoundedCornerShape(18.dp)
                            ) {
                                Column(Modifier.padding(20.dp)) {
                                    Text(
                                        "مرحباً بك في وكيل بلس",
                                        color = Color.White,
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        "إدارة أعمالك التموينية من مكان واحد",
                                        color = Color.White
                                    )
                                }
                            }

                            Spacer(Modifier.height(20.dp))

                            Text(
                                "أقسام النظام",
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(Modifier.height(12.dp))

                            LazyVerticalGrid(
                                columns = GridCells.Fixed(2),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                gridItems(sections) { section ->
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(112.dp)
                                            .clickable {
                                                selectedSection = section.title
                                            },
                                        shape = RoundedCornerShape(16.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = Color.White
                                        ),
                                        elevation = CardDefaults.cardElevation(2.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(12.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Text(section.icon, fontSize = 28.sp)
                                            Spacer(Modifier.height(8.dp))
                                            Text(
                                                section.title,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = section.color
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (showForm) {
                BeneficiaryForm(
                    person = editingPerson,
                    onDismiss = { showForm = false },
                    onSave = { person ->
                        val old = beneficiaries.indexOfFirst {
                            it.id == person.id
                        }

                        if (old >= 0) {
                            beneficiaries[old] = person
                        } else {
                            beneficiaries.add(person)
                        }

                        saveBeneficiaries(context, beneficiaries)
                        showForm = false
                    }
                )
            }
        }
    }
}

@Composable
fun BeneficiaryForm(
    person: Beneficiary?,
    onDismiss: () -> Unit,
    onSave: (Beneficiary) -> Unit
) {
    var name by remember(person) { mutableStateOf(person?.name ?: "") }
    var address by remember(person) { mutableStateOf(person?.address ?: "") }
    var phone by remember(person) { mutableStateOf(person?.phone ?: "") }
    var members by remember(person) { mutableStateOf(person?.members ?: "") }
    var cardNumber by remember(person) { mutableStateOf(person?.cardNumber ?: "") }
    var nfc by remember(person) { mutableStateOf(person?.nfc ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (person == null) "إضافة مستفيد جديد"
                else "تعديل بيانات المستفيد"
            )
        },
        text = {
            LazyColumn(
                modifier = Modifier.heightIn(max = 430.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    OutlinedTextField(
                        name, { name = it },
                        label = { Text("الاسم الكامل") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
                item {
                    OutlinedTextField(
                        address, { address = it },
                        label = { Text("العنوان") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        phone, { phone = it },
                        label = { Text("رقم واتساب / الهاتف") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
                item {
                    OutlinedTextField(
                        members, { members = it },
                        label = { Text("عدد أفراد الأسرة") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
                item {
                    OutlinedTextField(
                        cardNumber, { cardNumber = it },
                        label = { Text("رقم البطاقة التموينية") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
                item {
                    OutlinedTextField(
                        nfc, { nfc = it },
                        label = { Text("رقم بطاقة NFC") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }
        },
        confirmButton = {
            Button(
                enabled = name.isNotBlank() &&
                    cardNumber.isNotBlank(),
                onClick = {
                    onSave(
                        Beneficiary(
                            id = person?.id ?: System.currentTimeMillis(),
                            name = name.trim(),
                            address = address.trim(),
                            phone = phone.trim(),
                            members = members.trim(),
                            cardNumber = cardNumber.trim(),
                            nfc = nfc.trim()
                        )
                    )
                }
            ) {
                Text("حفظ المستفيد")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}
