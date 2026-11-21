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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { WakilPlusApp() }
    }
}

data class AppSection(val title: String, val icon: String, val color: Color)

data class Beneficiary(
    val id: Long,
    val name: String,
    val address: String,
    val phone: String,
    val members: String,
    val cardNumber: String,
    val nfc: String
)

data class FoodItem(
    val id: Long,
    val name: String,
    val unit: String,
    val perPerson: Double
)

data class DeliveryLine(
    val itemId: Long,
    val itemName: String,
    val quantity: Double
)

data class FoodDelivery(
    val id: Long,
    val cardNumber: String,
    val familyName: String,
    val date: String,
    val lines: List<DeliveryLine>
)

data class SocialCase(
    val id: Long,
    val cardNumber: String,
    val familyName: String,
    val monthlyAmount: Double
)

data class SocialPayment(
    val id: Long,
    val cardNumber: String,
    val familyName: String,
    val date: String,
    val amount: Double
)
data class DebtRecord(
    val id: Long,
    val cardNumber: String,
    val familyName: String,
    val description: String,
    val date: String,
    val amount: Double
)

data class DebtPayment(
    val id: Long,
    val cardNumber: String,
    val familyName: String,
    val date: String,
    val amount: Double,
    val note: String
)
const val BENEFICIARIES_KEY = "wakilplus_beneficiaries"
const val FOOD_ITEMS_KEY = "wakilplus_food_items"
const val FOOD_DELIVERIES_KEY = "wakilplus_food_deliveries"
const val FLOUR_ITEMS_KEY = "wakilplus_flour_items"
const val FLOUR_DELIVERIES_KEY = "wakilplus_flour_deliveries"
const val SOCIAL_CASES_KEY = "wakilplus_social_cases"
const val SOCIAL_PAYMENTS_KEY = "wakilplus_social_payments"
const val DEBT_RECORDS_KEY = "wakilplus_debt_records"
const val DEBT_PAYMENTS_KEY = "wakilplus_debt_payments"
fun prefs(context: Context) =
    context.getSharedPreferences("WakilPlusData", Context.MODE_PRIVATE)

fun loadBeneficiaries(context: Context): List<Beneficiary> {
    return try {
        val array = JSONArray(prefs(context).getString(BENEFICIARIES_KEY, "[]"))
        val result = mutableListOf<Beneficiary>()
        for (i in 0 until array.length()) {
            val o = array.getJSONObject(i)
            result.add(
                Beneficiary(
                    o.optLong("id"), o.optString("name"),
                    o.optString("address"), o.optString("phone"),
                    o.optString("members"), o.optString("cardNumber"),
                    o.optString("nfc")
                )
            )
        }
        result
    } catch (_: Exception) { emptyList() }
}

fun saveBeneficiaries(context: Context, list: List<Beneficiary>) {
    val array = JSONArray()
    list.forEach {
        val o = JSONObject()
        o.put("id", it.id)
        o.put("name", it.name)
        o.put("address", it.address)
        o.put("phone", it.phone)
        o.put("members", it.members)
        o.put("cardNumber", it.cardNumber)
        o.put("nfc", it.nfc)
        array.put(o)
    }
    prefs(context).edit().putString(BENEFICIARIES_KEY, array.toString()).apply()
}

fun loadItems(context: Context, key: String): List<FoodItem> {
    return try {
        val array = JSONArray(prefs(context).getString(key, "[]"))
        val result = mutableListOf<FoodItem>()
        for (i in 0 until array.length()) {
            val o = array.getJSONObject(i)
            result.add(
                FoodItem(
                    o.optLong("id"), o.optString("name"),
                    o.optString("unit"), o.optDouble("perPerson")
                )
            )
        }
        result
    } catch (_: Exception) { emptyList() }
}

fun saveItems(context: Context, key: String, list: List<FoodItem>) {
    val array = JSONArray()
    list.forEach {
        val o = JSONObject()
        o.put("id", it.id)
        o.put("name", it.name)
        o.put("unit", it.unit)
        o.put("perPerson", it.perPerson)
        array.put(o)
    }
    prefs(context).edit().putString(key, array.toString()).apply()
}

fun loadDeliveries(context: Context, key: String): List<FoodDelivery> {
    return try {
        val array = JSONArray(prefs(context).getString(key, "[]"))
        val result = mutableListOf<FoodDelivery>()
        for (i in 0 until array.length()) {
            val o = array.getJSONObject(i)
            val linesArray = o.optJSONArray("lines") ?: JSONArray()
            val lines = mutableListOf<DeliveryLine>()
            for (j in 0 until linesArray.length()) {
                val line = linesArray.getJSONObject(j)
                lines.add(
                    DeliveryLine(
                        line.optLong("itemId"),
                        line.optString("itemName"),
                        line.optDouble("quantity")
                    )
                )
            }
            result.add(
                FoodDelivery(
                    o.optLong("id"), o.optString("cardNumber"),
                    o.optString("familyName"), o.optString("date"), lines
                )
            )
        }
        result
    } catch (_: Exception) { emptyList() }
}

fun saveDeliveries(context: Context, key: String, list: List<FoodDelivery>) {
    val array = JSONArray()
    list.forEach { delivery ->
        val o = JSONObject()
        o.put("id", delivery.id)
        o.put("cardNumber", delivery.cardNumber)
        o.put("familyName", delivery.familyName)
        o.put("date", delivery.date)
        val lines = JSONArray()
        delivery.lines.forEach {
            val line = JSONObject()
            line.put("itemId", it.itemId)
            line.put("itemName", it.itemName)
            line.put("quantity", it.quantity)
            lines.put(line)
        }
        o.put("lines", lines)
        array.put(o)
    }
    prefs(context).edit().putString(key, array.toString()).apply()
}

fun loadSocialCases(context: Context): List<SocialCase> {
    return try {
        val array = JSONArray(prefs(context).getString(SOCIAL_CASES_KEY, "[]"))
        val result = mutableListOf<SocialCase>()
        for (i in 0 until array.length()) {
            val o = array.getJSONObject(i)
            result.add(
                SocialCase(
                    o.optLong("id"),
                    o.optString("cardNumber"),
                    o.optString("familyName"),
                    o.optDouble("monthlyAmount")
                )
            )
        }
        result
    } catch (_: Exception) { emptyList() }
}

fun saveSocialCases(context: Context, list: List<SocialCase>) {
    val array = JSONArray()
    list.forEach {
        val o = JSONObject()
        o.put("id", it.id)
        o.put("cardNumber", it.cardNumber)
        o.put("familyName", it.familyName)
        o.put("monthlyAmount", it.monthlyAmount)
        array.put(o)
    }
    prefs(context).edit().putString(SOCIAL_CASES_KEY, array.toString()).apply()
}

fun loadSocialPayments(context: Context): List<SocialPayment> {
    return try {
        val array = JSONArray(prefs(context).getString(SOCIAL_PAYMENTS_KEY, "[]"))
        val result = mutableListOf<SocialPayment>()
        for (i in 0 until array.length()) {
            val o = array.getJSONObject(i)
            result.add(
                SocialPayment(
                    o.optLong("id"),
                    o.optString("cardNumber"),
                    o.optString("familyName"),
                    o.optString("date"),
                    o.optDouble("amount")
                )
            )
        }
        result
    } catch (_: Exception) { emptyList() }
}

fun saveSocialPayments(context: Context, list: List<SocialPayment>) {
    val array = JSONArray()
    list.forEach {
        val o = JSONObject()
        o.put("id", it.id)
        o.put("cardNumber", it.cardNumber)
        o.put("familyName", it.familyName)
        o.put("date", it.date)
        o.put("amount", it.amount)
        array.put(o)
    }
    prefs(context).edit().putString(SOCIAL_PAYMENTS_KEY, array.toString()).apply()
}
fun loadDebtRecords(context: Context): List<DebtRecord> {
    return try {
        val array = JSONArray(
            prefs(context).getString(DEBT_RECORDS_KEY, "[]")
        )
        val result = mutableListOf<DebtRecord>()

        for (i in 0 until array.length()) {
            val o = array.getJSONObject(i)
            result.add(
                DebtRecord(
                    o.optLong("id"),
                    o.optString("cardNumber"),
                    o.optString("familyName"),
                    o.optString("description"),
                    o.optString("date"),
                    o.optDouble("amount")
                )
            )
        }
        result
    } catch (_: Exception) {
        emptyList()
    }
}

fun saveDebtRecords(context: Context, list: List<DebtRecord>) {
    val array = JSONArray()
    list.forEach {
        val o = JSONObject()
        o.put("id", it.id)
        o.put("cardNumber", it.cardNumber)
        o.put("familyName", it.familyName)
        o.put("description", it.description)
        o.put("date", it.date)
        o.put("amount", it.amount)
        array.put(o)
    }
    prefs(context).edit()
        .putString(DEBT_RECORDS_KEY, array.toString())
        .apply()
}

fun loadDebtPayments(context: Context): List<DebtPayment> {
    return try {
        val array = JSONArray(
            prefs(context).getString(DEBT_PAYMENTS_KEY, "[]")
        )
        val result = mutableListOf<DebtPayment>()

        for (i in 0 until array.length()) {
            val o = array.getJSONObject(i)
            result.add(
                DebtPayment(
                    o.optLong("id"),
                    o.optString("cardNumber"),
                    o.optString("familyName"),
                    o.optString("date"),
                    o.optDouble("amount"),
                    o.optString("note")
                )
            )
        }
        result
    } catch (_: Exception) {
        emptyList()
    }
}

fun saveDebtPayments(context: Context, list: List<DebtPayment>) {
    val array = JSONArray()
    list.forEach {
        val o = JSONObject()
        o.put("id", it.id)
        o.put("cardNumber", it.cardNumber)
        o.put("familyName", it.familyName)
        o.put("date", it.date)
        o.put("amount", it.amount)
        o.put("note", it.note)
        array.put(o)
    }
    prefs(context).edit()
        .putString(DEBT_PAYMENTS_KEY, array.toString())
        .apply()
}
fun deliveredAmount(
    deliveries: List<FoodDelivery>,
    card: String,
    itemId: Long
): Double {
    return deliveries.filter { it.cardNumber == card }
        .flatMap { it.lines }
        .filter { it.itemId == itemId }
        .sumOf { it.quantity }
}

fun socialPaid(payments: List<SocialPayment>, card: String): Double {
    return payments.filter { it.cardNumber == card }.sumOf { it.amount }
}

fun quantityText(value: Double): String =
    if (value % 1.0 == 0.0) value.toLong().toString()
    else String.format(Locale.US, "%.2f", value)

@Composable
fun WakilPlusApp() {
    val context = LocalContext.current

    val beneficiaries = remember {
        mutableStateListOf<Beneficiary>().apply {
            addAll(loadBeneficiaries(context))
        }
    }
    val foodItems = remember {
        mutableStateListOf<FoodItem>().apply {
            addAll(loadItems(context, FOOD_ITEMS_KEY))
        }
    }
    val foodDeliveries = remember {
        mutableStateListOf<FoodDelivery>().apply {
            addAll(loadDeliveries(context, FOOD_DELIVERIES_KEY))
        }
    }
    val flourItems = remember {
        mutableStateListOf<FoodItem>().apply {
            addAll(loadItems(context, FLOUR_ITEMS_KEY))
        }
    }
    val flourDeliveries = remember {
        mutableStateListOf<FoodDelivery>().apply {
            addAll(loadDeliveries(context, FLOUR_DELIVERIES_KEY))
        }
    }
    val socialCases = remember {
        mutableStateListOf<SocialCase>().apply {
            addAll(loadSocialCases(context))
        }
    }
    val socialPayments = remember {
        mutableStateListOf<SocialPayment>().apply {
            addAll(loadSocialPayments(context))
        }
    }
val debtRecords = remember {
    mutableStateListOf<DebtRecord>().apply {
        addAll(loadDebtRecords(context))
    }
}

val debtPayments = remember {
    mutableStateListOf<DebtPayment>().apply {
        addAll(loadDebtPayments(context))
    }
}
    var section by remember { mutableStateOf("") }
    var search by remember { mutableStateOf("") }
    var editingPerson by remember { mutableStateOf<Beneficiary?>(null) }
    var showPersonForm by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<FoodItem?>(null) }
    var showItemForm by remember { mutableStateOf(false) }
    var deliveryFamily by remember { mutableStateOf<Beneficiary?>(null) }
    var socialFamily by remember { mutableStateOf<Beneficiary?>(null) }
    var editingSocial by remember { mutableStateOf<SocialCase?>(null) }
    var showSocialForm by remember { mutableStateOf(false) }
    var paymentCase by remember { mutableStateOf<SocialCase?>(null) }
    var debtFamily by remember { mutableStateOf<Beneficiary?>(null) }
var debtAction by remember { mutableStateOf("") }
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
                        modifier = Modifier.fillMaxWidth()
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
                        Text("نظام الوكيل التمويني", color = Color.White)
                    }
                }
            ) { padding ->
                when (section) {
                    "المستفيدون" -> {
                        BeneficiariesScreen(
                            beneficiaries, search, { search = it },
                            { section = "" },
                            {
                                editingPerson = null
                                showPersonForm = true
                            },
                            {
                                editingPerson = it
                                showPersonForm = true
                            },
                            {
                                beneficiaries.remove(it)
                                saveBeneficiaries(context, beneficiaries)
                            },
                            Modifier.padding(padding)
                        )
                    }

                    "الحصة الغذائية", "مادة الطحين" -> {
                        val isFlour = section == "مادة الطحين"
                        val itemsList = if (isFlour) flourItems else foodItems
                        val deliveryList =
                            if (isFlour) flourDeliveries else foodDeliveries
                        val itemKey =
                            if (isFlour) FLOUR_ITEMS_KEY else FOOD_ITEMS_KEY
                        val deliveryKey =
                            if (isFlour) FLOUR_DELIVERIES_KEY else FOOD_DELIVERIES_KEY

                        QuotaScreen(
                            section, beneficiaries, itemsList, deliveryList,
                            search, { search = it }, { section = "" },
                            {
                                editingItem = null
                                showItemForm = true
                            },
                            {
                                editingItem = it
                                showItemForm = true
                            },
                            {
                                itemsList.remove(it)
                                saveItems(context, itemKey, itemsList)
                            },
                            { deliveryFamily = it },
                            Modifier.padding(padding)
                        )

                        if (showItemForm) {
                            FoodItemForm(
                                editingItem,
                                if (isFlour) "مادة الطحين" else "الحصة الغذائية",
                                { showItemForm = false }
                            ) { item ->
                                val index = itemsList.indexOfFirst { it.id == item.id }
                                if (index >= 0) itemsList[index] = item
                                else itemsList.add(item)
                                saveItems(context, itemKey, itemsList)
                                showItemForm = false
                            }
                        }

                        deliveryFamily?.let { family ->
                            FoodDeliveryForm(
                                family, itemsList.toList(), deliveryList.toList(),
                                section, { deliveryFamily = null }
                            ) { delivery ->
                                deliveryList.add(delivery)
                                saveDeliveries(context, deliveryKey, deliveryList)
                                deliveryFamily = null
                            }
                        }
                    }

                    "الرعاية الاجتماعية" -> {
                        SocialScreen(
                            beneficiaries = beneficiaries,
                            cases = socialCases,
                            payments = socialPayments,
                            search = search,
                            onSearch = { search = it },
                            onBack = { section = "" },
                            onAdd = {
                                socialFamily = null
                                editingSocial = null
                                showSocialForm = true
                            },
                            onEdit = { social ->
    socialFamily = beneficiaries.find {
        it.cardNumber == social.cardNumber
    }
    editingSocial = social
    showSocialForm = true
},
onPay = { paymentCase = it },
                            modifier = Modifier.padding(padding)
                        )

                        if (showSocialForm) {
                            SocialCaseForm(
                                beneficiaries = beneficiaries,
                                existing = editingSocial,
                                onDismiss = { showSocialForm = false }
                            ) { socialCase ->
                                val index = socialCases.indexOfFirst {
                                    it.id == socialCase.id
                                }
                                if (index >= 0) socialCases[index] = socialCase
                                else socialCases.add(socialCase)
                                saveSocialCases(context, socialCases)
                                showSocialForm = false
                            }
                        }

                        paymentCase?.let { social ->
                            SocialPaymentForm(
                                social = social,
                                paid = socialPaid(socialPayments, social.cardNumber),
                                onDismiss = { paymentCase = null }
                            ) { payment ->
                                socialPayments.add(payment)
                                saveSocialPayments(context, socialPayments)
                                paymentCase = null
                            }
                        }
                    }

                    else -> {
                        Column(
                            modifier = Modifier.fillMaxSize()
                                .padding(padding).padding(16.dp)
                        ) {
                            if (section.isNotEmpty()) {
                                TextButton(onClick = { section = "" }) {
                                    Text("← العودة للرئيسية")
                                }
                                Text(
                                    section,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.height(12.dp))
                                Text("سيتم تجهيز هذا القسم في المراحل القادمة.")
                            } else {
                                Text(
                                    "لوحة التحكم الرئيسية",
                                    fontSize = 21.sp,
                                    fontWeight = FontWeight.Bold
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
                                        Text(
                                            "إدارة أعمالك التموينية من مكان واحد",
                                            color = Color.White
                                        )
                                    }
                                }
                                Spacer(Modifier.height(20.dp))
                                Text("أقسام النظام", fontSize = 19.sp)
                                Spacer(Modifier.height(12.dp))
                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(2),
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    gridItems(sections) { item ->
                                        Card(
                                            modifier = Modifier.fillMaxWidth()
                                                .height(112.dp)
                                                .clickable {
                                                    section = item.title
                                                    search = ""
                                                },
                                            colors = CardDefaults.cardColors(
                                                containerColor = Color.White
                                            ),
                                            shape = RoundedCornerShape(16.dp)
                                        ) {
                                            Column(
                                                modifier = Modifier.fillMaxSize()
                                                    .padding(12.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.Center
                                            ) {
                                                Text(item.icon, fontSize = 28.sp)
                                                Spacer(Modifier.height(8.dp))
                                                Text(
                                                    item.title,
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = item.color
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (showPersonForm) {
                BeneficiaryForm(
                    editingPerson,
                    { showPersonForm = false }
                ) { person ->
                    val index = beneficiaries.indexOfFirst { it.id == person.id }
                    if (index >= 0) beneficiaries[index] = person
                    else beneficiaries.add(person)
                    saveBeneficiaries(context, beneficiaries)
                    showPersonForm = false
                }
            }
        }
    }
}

@Composable
fun BeneficiariesScreen(
    beneficiaries: List<Beneficiary>,
    search: String,
    onSearch: (String) -> Unit,
    onBack: () -> Unit,
    onAdd: () -> Unit,
    onEdit: (Beneficiary) -> Unit,
    onDelete: (Beneficiary) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier.fillMaxSize().padding(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "سجل المستفيدين",
                fontSize = 21.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onBack) { Text("الرئيسية") }
        }
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF126B53))
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("عدد المستفيدين", color = Color.White)
                Text(
                    beneficiaries.size.toString(),
                    color = Color.White,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            search, onSearch,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("ابحث بالاسم أو رقم البطاقة") },
            singleLine = true
        )
        Button(onClick = onAdd, modifier = Modifier.fillMaxWidth()) {
            Text("＋ إضافة مستفيد جديد")
        }
        val filtered = beneficiaries.filter {
            it.name.contains(search, true) ||
                it.cardNumber.contains(search, true) ||
                it.phone.contains(search, true)
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(filtered, key = { it.id }) { person ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text(person.name, fontWeight = FontWeight.Bold)
                        Text("العنوان: ${person.address}")
                        Text("الهاتف: ${person.phone}")
                        Text("عدد أفراد الأسرة: ${person.members}")
                        Text("رقم البطاقة: ${person.cardNumber}")
                        Text("رقم NFC: ${person.nfc}")
                        Row {
                            TextButton(onClick = { onEdit(person) }) {
                                Text("تعديل")
                            }
                            TextButton(onClick = { onDelete(person) }) {
                                Text("حذف", color = Color.Red)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun QuotaScreen(
    title: String,
    beneficiaries: List<Beneficiary>,
    quotaItems: List<FoodItem>,
    deliveries: List<FoodDelivery>,
    search: String,
    onSearch: (String) -> Unit,
    onBack: () -> Unit,
    onAddItem: () -> Unit,
    onEditItem: (FoodItem) -> Unit,
    onDeleteItem: (FoodItem) -> Unit,
    onStartDelivery: (Beneficiary) -> Unit,
    modifier: Modifier = Modifier
) {
    val isFlour = title == "مادة الطحين"
    val mainColor = if (isFlour) Color(0xFFD18B26) else Color(0xFF2878C7)

    Column(modifier.fillMaxSize().padding(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                title,
                fontSize = 21.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onBack) { Text("الرئيسية") }
        }
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = mainColor)
        ) {
            Column(Modifier.padding(14.dp)) {
                Text("سجل $title", color = Color.White)
                Text(
                    "عدد المواد: ${quotaItems.size}",
                    color = Color.White,
                    fontSize = 23.sp,
                    fontWeight = FontWeight.Bold
                )
                Text("عمليات التسليم: ${deliveries.size}", color = Color.White)
            }
        }
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = onAddItem,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = mainColor)
        ) {
            Text("＋ إضافة مادة جديدة")
        }
        Text("المواد المسجلة", fontWeight = FontWeight.Bold)
        if (quotaItems.isEmpty()) {
            Text("لم تتم إضافة مواد بعد.")
        } else {
            LazyColumn(
                modifier = Modifier.heightIn(max = 190.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                items(quotaItems, key = { it.id }) { item ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(item.name, fontWeight = FontWeight.Bold)
                                Text(
                                    "استحقاق الفرد: ${quantityText(item.perPerson)} ${item.unit}"
                                )
                            }
                            TextButton(onClick = { onEditItem(item) }) {
                                Text("تعديل")
                            }
                            TextButton(onClick = { onDeleteItem(item) }) {
                                Text("حذف", color = Color.Red)
                            }
                        }
                    }
                }
            }
        }
        Divider(Modifier.padding(vertical = 8.dp))
        Text(
            "سجل مستفيدي ${if (isFlour) "الطحين" else "الحصة الغذائية"}",
            fontWeight = FontWeight.Bold,
            fontSize = 17.sp
        )
        OutlinedTextField(
            search, onSearch,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("ابحث بالاسم أو رقم البطاقة التموينية") },
            singleLine = true
        )
        val matches = beneficiaries.filter {
            search.isNotBlank() && (
                it.name.contains(search, true) ||
                    it.cardNumber.contains(search, true)
                )
        }
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(matches, key = { it.id }) { family ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(family.name, fontWeight = FontWeight.Bold)
                            Text("البطاقة: ${family.cardNumber}")
                            Text("أفراد الأسرة: ${family.members}")
                        }
                        Button(
                            enabled = quotaItems.isNotEmpty(),
                            onClick = { onStartDelivery(family) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = mainColor
                            )
                        ) { Text("تسليم") }
                    }
                }
            }
        }
        if (search.isNotBlank() && matches.isEmpty()) {
            Text("لم يتم العثور على عائلة مطابقة.")
        }
    }
}

@Composable
fun FoodItemForm(
    item: FoodItem?,
    title: String,
    onDismiss: () -> Unit,
    onSave: (FoodItem) -> Unit
) {
    var name by remember(item) { mutableStateOf(item?.name ?: "") }
    var unit by remember(item) { mutableStateOf(item?.unit ?: "كغم") }
    var amount by remember(item) {
        mutableStateOf(item?.perPerson?.let { quantityText(it) } ?: "")
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (item == null) "إضافة مادة" else "تعديل المادة") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("اسم المادة") })
                OutlinedTextField(unit, { unit = it }, label = { Text("وحدة القياس") })
                OutlinedTextField(
                    amount, { amount = it },
                    label = { Text("استحقاق الفرد الواحد") }
                )
            }
        },
        confirmButton = {
            Button(
                enabled = name.isNotBlank() && unit.isNotBlank() &&
                    (amount.toDoubleOrNull() ?: 0.0) > 0,
                onClick = {
                    onSave(
                        FoodItem(
                            item?.id ?: System.currentTimeMillis(),
                            name.trim(), unit.trim(), amount.toDouble()
                        )
                    )
                }
            ) { Text("حفظ المادة") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}

@Composable
fun FoodDeliveryForm(
    family: Beneficiary,
    quotaItems: List<FoodItem>,
    deliveries: List<FoodDelivery>,
    title: String,
    onDismiss: () -> Unit,
    onSave: (FoodDelivery) -> Unit
) {
    val memberCount = family.members.toDoubleOrNull() ?: 0.0
    val quantities = remember(family.id, quotaItems, deliveries) {
        mutableStateMapOf<Long, String>().apply {
            quotaItems.forEach { item ->
                val entitled = item.perPerson * memberCount
                val delivered = deliveredAmount(deliveries, family.cardNumber, item.id)
                put(
                    item.id,
                    quantityText((entitled - delivered).coerceAtLeast(0.0))
                )
            }
        }
    }
    val valid = quotaItems.isNotEmpty() &&
        quotaItems.all { item ->
            val remaining = (
                item.perPerson * memberCount -
                    deliveredAmount(deliveries, family.cardNumber, item.id)
                ).coerceAtLeast(0.0)
            val entered = quantities[item.id]?.toDoubleOrNull() ?: 0.0
            entered >= 0 && entered <= remaining
        } &&
        quotaItems.any { (quantities[it.id]?.toDoubleOrNull() ?: 0.0) > 0 }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تسليم $title") },
        text = {
            Column {
                Text(family.name, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text("رقم البطاقة: ${family.cardNumber}")
                Text("عدد أفراد الأسرة: ${family.members}")
                Spacer(Modifier.height(8.dp))
                LazyColumn(
                    modifier = Modifier.heightIn(max = 390.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(quotaItems, key = { it.id }) { item ->
                        val entitled = item.perPerson * memberCount
                        val already = deliveredAmount(
                            deliveries, family.cardNumber, item.id
                        )
                        val remaining = (entitled - already).coerceAtLeast(0.0)
                        Card {
                            Column(Modifier.padding(10.dp)) {
                                Text(item.name, fontWeight = FontWeight.Bold)
                                Text("الاستحقاق: ${quantityText(entitled)} ${item.unit}")
                                Text("المسلّم سابقاً: ${quantityText(already)} ${item.unit}")
                                Text(
                                    "المتبقي: ${quantityText(remaining)} ${item.unit}",
                                    color = Color(0xFF168B68)
                                )
                                OutlinedTextField(
                                    value = quantities[item.id] ?: "0",
                                    onValueChange = { quantities[item.id] = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    label = { Text("الكمية المسلّمة الآن (${item.unit})") },
                                    singleLine = true
                                )
                                val entered = quantities[item.id]?.toDoubleOrNull()
                                if (entered != null && entered > remaining) {
                                    Text("الكمية أكبر من المتبقي", color = Color.Red)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = valid,
                onClick = {
                    val lines = quotaItems.mapNotNull { item ->
                        val amount = quantities[item.id]?.toDoubleOrNull() ?: 0.0
                        if (amount > 0) DeliveryLine(item.id, item.name, amount)
                        else null
                    }
                    val date = SimpleDateFormat(
                        "yyyy/MM/dd - HH:mm", Locale.getDefault()
                    ).format(Date())
                    onSave(
                        FoodDelivery(
                            System.currentTimeMillis(),
                            family.cardNumber, family.name, date, lines
                        )
                    )
                }
            ) { Text("حفظ عملية التسليم") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
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
            Text(if (person == null) "إضافة مستفيد جديد" else "تعديل بيانات المستفيد")
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
                        modifier = Modifier.fillMaxWidth()
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
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        members, { members = it },
                        label = { Text("عدد أفراد الأسرة") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        cardNumber, { cardNumber = it },
                        label = { Text("رقم البطاقة التموينية") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        nfc, { nfc = it },
                        label = { Text("رقم بطاقة NFC") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                enabled = name.isNotBlank() && cardNumber.isNotBlank(),
                onClick = {
                    onSave(
                        Beneficiary(
                            person?.id ?: System.currentTimeMillis(),
                            name.trim(), address.trim(), phone.trim(),
                            members.trim(), cardNumber.trim(), nfc.trim()
                        )
                    )
                }
            ) { Text("حفظ المستفيد") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}

@Composable
fun SocialScreen(
    beneficiaries: List<Beneficiary>,
    cases: List<SocialCase>,
    payments: List<SocialPayment>,
    search: String,
    onSearch: (String) -> Unit,
    onBack: () -> Unit,
    onAdd: () -> Unit,
    onEdit: (SocialCase) -> Unit,
    onPay: (SocialCase) -> Unit,
    modifier: Modifier = Modifier
) {
    fun debtTotal(records: List<DebtRecord>, card: String): Double {
    return records.filter { it.cardNumber == card }.sumOf { it.amount }
}

fun debtPaid(payments: List<DebtPayment>, card: String): Double {
    return payments.filter { it.cardNumber == card }.sumOf { it.amount }
}
@Composable
fun DebtScreen(
    beneficiaries: List<Beneficiary>,
    records: List<DebtRecord>,
    payments: List<DebtPayment>,
    onAddDebt: (Beneficiary) -> Unit,
    onPayDebt: (Beneficiary) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val totalDebt = records.sumOf { it.amount }
    val totalPaid = payments.sumOf { it.amount }
    val remaining = (totalDebt - totalPaid).coerceAtLeast(0.0)

    Column(
        modifier.fillMaxSize().padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "الديون والحسابات",
                fontSize = 21.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onBack) {
                Text("الرئيسية")
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF2878C7)
            )
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("إجمالي الديون: ${quantityText(totalDebt)}", color = Color.White)
                Text("إجمالي المدفوع: ${quantityText(totalPaid)}", color = Color.White)
                Text(
                    "المتبقي: ${quantityText(remaining)}",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        Text(
            "حسابات العوائل",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(beneficiaries, key = { it.id }) { family ->
                val debt = debtTotal(records, family.cardNumber)
                val paid = debtPaid(payments, family.cardNumber)
                val balance = (debt - paid).coerceAtLeast(0.0)

                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text(
                            family.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                        Text("رقم البطاقة: ${family.cardNumber}")
                        Text("إجمالي الدين: ${quantityText(debt)}")
                        Text("المدفوع: ${quantityText(paid)}")
                        Text(
                            "المتبقي: ${quantityText(balance)}",
                            color = if (balance > 0) Color.Red
                            else Color(0xFF168B68)
                        )

                        Row {
                            TextButton(
                                onClick = { onAddDebt(family) }
                            ) {
                                Text("＋ تسجيل دين")
                            }

                            TextButton(
                                enabled = balance > 0,
                                onClick = { onPayDebt(family) }
                            ) {
                                Text("تسجيل تسديد")
                            }
                        }

                        val history = records.filter {
                            it.cardNumber == family.cardNumber
                        }

                        if (history.isNotEmpty()) {
                            Divider()
                            Text(
                                "سجل الديون",
                                fontWeight = FontWeight.Bold
                            )
                            history.sortedByDescending { it.id }.forEach {
                                Text(
                                    "${it.date} • ${it.description} • ${quantityText(it.amount)}"
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DebtRecordForm(
    family: Beneficiary,
    onDismiss: () -> Unit,
    onSave: (DebtRecord) -> Unit
) {
    var description by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تسجيل دين جديد") },
        text = {
            Column {
                Text("العائلة: ${family.name}")
                Text("رقم البطاقة: ${family.cardNumber}")

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("وصف الدين") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("المبلغ") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = description.isNotBlank() &&
                    (amount.toDoubleOrNull() ?: 0.0) > 0,
                onClick = {
                    onSave(
                        DebtRecord(
                            id = System.currentTimeMillis(),
                            cardNumber = family.cardNumber,
                            familyName = family.name,
                            description = description.trim(),
                            date = SimpleDateFormat(
                                "yyyy/MM/dd - HH:mm",
                                Locale.getDefault()
                            ).format(Date()),
                            amount = amount.toDouble()
                        )
                    )
                }
            ) {
                Text("حفظ")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}

    val totalEntitlements = cases.sumOf { it.monthlyAmount }
    val totalPaid = cases.sumOf { socialPaid(payments, it.cardNumber) }

    Column(modifier.fillMaxSize().padding(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "الرعاية الاجتماعية",
                fontSize = 21.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onBack) { Text("الرئيسية") }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF8656B8))
        ) {
            Column(Modifier.padding(14.dp)) {
                Text("عدد العوائل المسجلة: ${cases.size}", color = Color.White)
                Text(
                    "مجموع الاستحقاقات: ${quantityText(totalEntitlements)}",
                    color = Color.White
                )
                Text(
                    "مجموع المدفوعات المسجلة: ${quantityText(totalPaid)}",
                    color = Color.White
                )
            }
        }

        Spacer(Modifier.height(8.dp))
        Button(
            onClick = onAdd,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8656B8))
        ) {
            Text("＋ إضافة مستفيد للرعاية الاجتماعية")
        }

        OutlinedTextField(
            value = search,
            onValueChange = onSearch,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("ابحث بالاسم أو رقم البطاقة التموينية") },
            singleLine = true
        )

        val filtered = cases.filter {
            it.familyName.contains(search, true) ||
                it.cardNumber.contains(search, true)
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(filtered, key = { it.id }) { social ->
                val paid = socialPaid(payments, social.cardNumber)
                val remaining = (social.monthlyAmount - paid).coerceAtLeast(0.0)
                val family = beneficiaries.find {
                    it.cardNumber == social.cardNumber
                }

                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text(social.familyName, fontWeight = FontWeight.Bold)
                        Text("رقم البطاقة: ${social.cardNumber}")
                        Text("عدد أفراد الأسرة: ${family?.members ?: "غير متوفر"}")
                        Text("الاستحقاق الشهري: ${quantityText(social.monthlyAmount)}")
                        Text("المبلغ المدفوع: ${quantityText(paid)}")
                        Text(
                            "المتبقي: ${quantityText(remaining)}",
                            color = if (remaining > 0) Color(0xFFB36B12)
                            else Color(0xFF168B68)
                        )
                        Row {
                            TextButton(onClick = { onEdit(social) }) {
                                Text("تعديل الاستحقاق")
                            }
                            TextButton(
                                enabled = remaining > 0,
                                onClick = { onPay(social) }
                            ) {
                                Text("تسجيل دفعة")
                            }
                        }
                        val history = payments.filter {
                            it.cardNumber == social.cardNumber
                        }
                        if (history.isNotEmpty()) {
                            Divider()
                            Text("سجل المدفوعات", fontWeight = FontWeight.Bold)
                            history.sortedByDescending { it.id }.forEach {
                                Text(
                                    "${it.date}  •  ${quantityText(it.amount)}"
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SocialPaymentForm(
    social: SocialCase,
    paid: Double,
    onDismiss: () -> Unit,
    onSave: (SocialPayment) -> Unit
) {
    var amount by remember(social.id) {
        mutableStateOf("")
    }

    val remaining = (social.monthlyAmount - paid).coerceAtLeast(0.0)
    val entered = amount.toDoubleOrNull() ?: 0.0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("تسجيل دفعة للرعاية الاجتماعية")
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(social.familyName, fontWeight = FontWeight.Bold)
                Text("رقم البطاقة: ${social.cardNumber}")
                Text("الاستحقاق الشهري: ${quantityText(social.monthlyAmount)}")
                Text("المدفوع سابقاً: ${quantityText(paid)}")
                Text("المتبقي: ${quantityText(remaining)}")

                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("المبلغ المدفوع الآن") },
                    singleLine = true
                )

                if (entered > remaining) {
                    Text(
                        "المبلغ أكبر من المتبقي",
                        color = Color.Red
                    )
                }
            }
        },
        confirmButton = {
            Button(
                enabled = entered > 0 && entered <= remaining,
                onClick = {
                    val date = SimpleDateFormat(
                        "yyyy/MM/dd - HH:mm",
                        Locale.getDefault()
                    ).format(Date())

                    onSave(
                        SocialPayment(
                            System.currentTimeMillis(),
                            social.cardNumber,
                            social.familyName,
                            date,
                            entered
                        )
                    )
                }
            ) {
                Text("حفظ الدفعة")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}
@Composable
fun SocialCaseForm(
    beneficiaries: List<Beneficiary>,
    existing: SocialCase?,
    onDismiss: () -> Unit,
    onSave: (SocialCase) -> Unit
) {
    var cardNumber by remember(existing) {
        mutableStateOf(existing?.cardNumber ?: "")
    }

    var familyName by remember(existing) {
        mutableStateOf(existing?.familyName ?: "")
    }

    var monthlyAmount by remember(existing) {
        mutableStateOf(
            existing?.monthlyAmount?.let { quantityText(it) } ?: ""
        )
    }

    val matchingFamily = beneficiaries.find {
        it.cardNumber == cardNumber.trim()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (existing == null)
                    "إضافة مستفيد للرعاية الاجتماعية"
                else
                    "تعديل الاستحقاق الاجتماعي"
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = cardNumber,
                    onValueChange = { value ->
                        cardNumber = value
                        val family = beneficiaries.find {
                            it.cardNumber == value.trim()
                        }
                        if (family != null) {
                            familyName = family.name
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("رقم البطاقة التموينية") },
                    singleLine = true
                )

                if (matchingFamily != null) {
                    Text(
                        "العائلة المسجلة: ${matchingFamily.name}",
                        color = Color(0xFF168B68)
                    )
                }

                OutlinedTextField(
                    value = familyName,
                    onValueChange = { familyName = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("اسم رب الأسرة") },
                    singleLine = true
                )

                OutlinedTextField(
                    value = monthlyAmount,
                    onValueChange = { monthlyAmount = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("الاستحقاق الشهري") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                enabled = cardNumber.isNotBlank() &&
                    familyName.isNotBlank() &&
                    (monthlyAmount.toDoubleOrNull() ?: 0.0) > 0,
                onClick = {
                    val socialCase = SocialCase(
                        id = existing?.id ?: System.currentTimeMillis(),
                        cardNumber = cardNumber.trim(),
                        familyName = familyName.trim(),
                        monthlyAmount = monthlyAmount.toDoubleOrNull() ?: 0.0
                    )
                    onSave(socialCase)
                }
            ) {
                Text("حفظ البيانات")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}
