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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
private const val PREFS_NAME = "WakilPlusData"
private const val BENEFICIARIES_KEY = "wakilplus_beneficiaries"
private const val FOOD_ITEMS_KEY = "wakilplus_food_items"
private const val FOOD_DELIVERIES_KEY = "wakilplus_food_deliveries"
private const val FLOUR_ITEMS_KEY = "wakilplus_flour_items"
private const val FLOUR_DELIVERIES_KEY = "wakilplus_flour_deliveries"
private const val SOCIAL_CASES_KEY = "wakilplus_social_cases"
private const val SOCIAL_PAYMENTS_KEY = "wakilplus_social_payments"
private const val DEBT_RECORDS_KEY = "wakilplus_debt_records"
private const val DEBT_PAYMENTS_KEY = "wakilplus_debt_payments"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { WakilPlusApp() }
    }
}

data class AppSection(val title: String, val icon: String, val color: Color)
data class Beneficiary(
    val id: Long, val name: String, val address: String, val phone: String,
    val members: String, val cardNumber: String, val nfc: String
)
data class FoodItem(val id: Long, val name: String, val unit: String, val perPerson: Double)
data class DeliveryLine(val itemId: Long, val itemName: String, val quantity: Double)
data class FoodDelivery(
    val id: Long, val cardNumber: String, val familyName: String,
    val date: String, val lines: List<DeliveryLine>
)
data class SocialCase(val id: Long, val cardNumber: String, val familyName: String, val monthlyAmount: Double)
data class SocialPayment(
    val id: Long, val cardNumber: String, val familyName: String,
    val date: String, val amount: Double
)
data class DebtRecord(
    val id: Long, val cardNumber: String, val familyName: String,
    val description: String, val date: String, val amount: Double
)
data class DebtPayment(
    val id: Long, val cardNumber: String, val familyName: String,
    val date: String, val amount: Double, val note: String
)

private fun prefs(context: Context) =
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

private fun readArray(context: Context, key: String): JSONArray =
    try { JSONArray(prefs(context).getString(key, "[]") ?: "[]") } catch (_: Exception) { JSONArray() }

private fun saveArray(context: Context, key: String, array: JSONArray) {
    prefs(context).edit().putString(key, array.toString()).apply()
}

private fun loadBeneficiaries(context: Context): List<Beneficiary> {
    val a = readArray(context, BENEFICIARIES_KEY)
    return (0 until a.length()).mapNotNull { i ->
        try {
            val o = a.getJSONObject(i)
            Beneficiary(o.optLong("id"), o.optString("name"), o.optString("address"),
                o.optString("phone"), o.optString("members"), o.optString("cardNumber"), o.optString("nfc"))
        } catch (_: Exception) { null }
    }
}

private fun saveBeneficiaries(context: Context, list: List<Beneficiary>) {
    val a = JSONArray()
    list.forEach { p ->
        a.put(JSONObject().put("id", p.id).put("name", p.name).put("address", p.address)
            .put("phone", p.phone).put("members", p.members).put("cardNumber", p.cardNumber).put("nfc", p.nfc))
    }
    saveArray(context, BENEFICIARIES_KEY, a)
}

private fun loadItems(context: Context, key: String): List<FoodItem> {
    val a = readArray(context, key)
    return (0 until a.length()).mapNotNull { i ->
        try {
            val o = a.getJSONObject(i)
            FoodItem(o.optLong("id"), o.optString("name"), o.optString("unit"), o.optDouble("perPerson"))
        } catch (_: Exception) { null }
    }
}

private fun saveItems(context: Context, key: String, list: List<FoodItem>) {
    val a = JSONArray()
    list.forEach { a.put(JSONObject().put("id", it.id).put("name", it.name).put("unit", it.unit).put("perPerson", it.perPerson)) }
    saveArray(context, key, a)
}

private fun loadDeliveries(context: Context, key: String): List<FoodDelivery> {
    val a = readArray(context, key)
    return (0 until a.length()).mapNotNull { i ->
        try {
            val o = a.getJSONObject(i)
            val la = o.optJSONArray("lines") ?: JSONArray()
            val lines = (0 until la.length()).mapNotNull { j ->
                try {
                    val x = la.getJSONObject(j)
                    DeliveryLine(x.optLong("itemId"), x.optString("itemName"), x.optDouble("quantity"))
                } catch (_: Exception) { null }
            }
            FoodDelivery(o.optLong("id"), o.optString("cardNumber"), o.optString("familyName"), o.optString("date"), lines)
        } catch (_: Exception) { null }
    }
}

private fun saveDeliveries(context: Context, key: String, list: List<FoodDelivery>) {
    val a = JSONArray()
    list.forEach { d ->
        val lines = JSONArray()
        d.lines.forEach { lines.put(JSONObject().put("itemId", it.itemId).put("itemName", it.itemName).put("quantity", it.quantity)) }
        a.put(JSONObject().put("id", d.id).put("cardNumber", d.cardNumber).put("familyName", d.familyName)
            .put("date", d.date).put("lines", lines))
    }
    saveArray(context, key, a)
}

private fun loadSocialCases(context: Context): List<SocialCase> {
    val a = readArray(context, SOCIAL_CASES_KEY)
    return (0 until a.length()).mapNotNull { i ->
        try { val o = a.getJSONObject(i); SocialCase(o.optLong("id"), o.optString("cardNumber"), o.optString("familyName"), o.optDouble("monthlyAmount")) }
        catch (_: Exception) { null }
    }
}
private fun saveSocialCases(context: Context, list: List<SocialCase>) {
    val a = JSONArray()
    list.forEach { a.put(JSONObject().put("id", it.id).put("cardNumber", it.cardNumber).put("familyName", it.familyName).put("monthlyAmount", it.monthlyAmount)) }
    saveArray(context, SOCIAL_CASES_KEY, a)
}
private fun loadSocialPayments(context: Context): List<SocialPayment> {
    val a = readArray(context, SOCIAL_PAYMENTS_KEY)
    return (0 until a.length()).mapNotNull { i ->
        try { val o = a.getJSONObject(i); SocialPayment(o.optLong("id"), o.optString("cardNumber"), o.optString("familyName"), o.optString("date"), o.optDouble("amount")) }
        catch (_: Exception) { null }
    }
}
private fun saveSocialPayments(context: Context, list: List<SocialPayment>) {
    val a = JSONArray()
    list.forEach { a.put(JSONObject().put("id", it.id).put("cardNumber", it.cardNumber).put("familyName", it.familyName).put("date", it.date).put("amount", it.amount)) }
    saveArray(context, SOCIAL_PAYMENTS_KEY, a)
}
private fun loadDebtRecords(context: Context): List<DebtRecord> {
    val a = readArray(context, DEBT_RECORDS_KEY)
    return (0 until a.length()).mapNotNull { i ->
        try { val o = a.getJSONObject(i); DebtRecord(o.optLong("id"), o.optString("cardNumber"), o.optString("familyName"), o.optString("description"), o.optString("date"), o.optDouble("amount")) }
        catch (_: Exception) { null }
    }
}
private fun saveDebtRecords(context: Context, list: List<DebtRecord>) {
    val a = JSONArray()
    list.forEach { a.put(JSONObject().put("id", it.id).put("cardNumber", it.cardNumber).put("familyName", it.familyName)
        .put("description", it.description).put("date", it.date).put("amount", it.amount)) }
    saveArray(context, DEBT_RECORDS_KEY, a)
}
private fun loadDebtPayments(context: Context): List<DebtPayment> {
    val a = readArray(context, DEBT_PAYMENTS_KEY)
    return (0 until a.length()).mapNotNull { i ->
        try { val o = a.getJSONObject(i); DebtPayment(o.optLong("id"), o.optString("cardNumber"), o.optString("familyName"), o.optString("date"), o.optDouble("amount"), o.optString("note")) }
        catch (_: Exception) { null }
    }
}
private fun saveDebtPayments(context: Context, list: List<DebtPayment>) {
    val a = JSONArray()
    list.forEach { a.put(JSONObject().put("id", it.id).put("cardNumber", it.cardNumber).put("familyName", it.familyName)
        .put("date", it.date).put("amount", it.amount).put("note", it.note)) }
    saveArray(context, DEBT_PAYMENTS_KEY, a)
}

private fun quantityText(value: Double): String =
    if (value % 1.0 == 0.0) value.toLong().toString() else String.format(Locale.US, "%.2f", value)
private fun nowText(): String = SimpleDateFormat("yyyy/MM/dd - HH:mm", Locale.getDefault()).format(Date())
private fun deliveredAmount(deliveries: List<FoodDelivery>, card: String, itemId: Long): Double =
    deliveries.filter { it.cardNumber == card }.flatMap { it.lines }.filter { it.itemId == itemId }.sumOf { it.quantity }
private fun socialPaid(payments: List<SocialPayment>, card: String): Double =
    payments.filter { it.cardNumber == card }.sumOf { it.amount }
private fun debtTotal(records: List<DebtRecord>, card: String): Double =
    records.filter { it.cardNumber == card }.sumOf { it.amount }
private fun debtPaid(payments: List<DebtPayment>, card: String): Double =
    payments.filter { it.cardNumber == card }.sumOf { it.amount }

@Composable
fun WakilPlusApp() {
    val context = LocalContext.current
    val beneficiaries = remember { mutableStateListOf<Beneficiary>().apply { addAll(loadBeneficiaries(context)) } }
    val foodItems = remember { mutableStateListOf<FoodItem>().apply { addAll(loadItems(context, FOOD_ITEMS_KEY)) } }
    val flourItems = remember { mutableStateListOf<FoodItem>().apply { addAll(loadItems(context, FLOUR_ITEMS_KEY)) } }
    val foodDeliveries = remember { mutableStateListOf<FoodDelivery>().apply { addAll(loadDeliveries(context, FOOD_DELIVERIES_KEY)) } }
    val flourDeliveries = remember { mutableStateListOf<FoodDelivery>().apply { addAll(loadDeliveries(context, FLOUR_DELIVERIES_KEY)) } }
    val socialCases = remember { mutableStateListOf<SocialCase>().apply { addAll(loadSocialCases(context)) } }
    val socialPayments = remember { mutableStateListOf<SocialPayment>().apply { addAll(loadSocialPayments(context)) } }
    val debtRecords = remember { mutableStateListOf<DebtRecord>().apply { addAll(loadDebtRecords(context)) } }
    val debtPayments = remember { mutableStateListOf<DebtPayment>().apply { addAll(loadDebtPayments(context)) } }

    var section by remember { mutableStateOf("") }
    var search by remember { mutableStateOf("") }
    var editingPerson by remember { mutableStateOf<Beneficiary?>(null) }
    var showPersonForm by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<FoodItem?>(null) }
    var showItemForm by remember { mutableStateOf(false) }
    var deliveryFamily by remember { mutableStateOf<Beneficiary?>(null) }
    var deliveryIsFlour by remember { mutableStateOf(false) }
    var socialEditing by remember { mutableStateOf<SocialCase?>(null) }
    var showSocialForm by remember { mutableStateOf(false) }
    var socialPaying by remember { mutableStateOf<SocialCase?>(null) }
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
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Scaffold(
                containerColor = Color(0xFFF3F6F8),
                topBar = {
                    Row(
                        Modifier.fillMaxWidth().background(Color(0xFF126B53)).padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("وكيل بلس", color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.weight(1f))
                        Text("نظام الوكيل التمويني", color = Color.White)
                    }
                }
            ) { padding ->
                when (section) {
                    "المستفيدون" -> BeneficiariesScreen(
                        beneficiaries, search, { search = it }, { section = "" },
                        { editingPerson = null; showPersonForm = true },
                        { editingPerson = it; showPersonForm = true },
                        { beneficiaries.remove(it); saveBeneficiaries(context, beneficiaries) },
                        Modifier.padding(padding)
                    )
                    "الحصة الغذائية", "مادة الطحين" -> {
                        val flour = section == "مادة الطحين"
                        val itemList = if (flour) flourItems else foodItems
                        val deliveryList = if (flour) flourDeliveries else foodDeliveries
                        val itemKey = if (flour) FLOUR_ITEMS_KEY else FOOD_ITEMS_KEY
                        val deliveryKey = if (flour) FLOUR_DELIVERIES_KEY else FOOD_DELIVERIES_KEY
                        QuotaScreen(section, beneficiaries, itemList, deliveryList, search, { search = it },
                            { section = "" }, { editingItem = null; showItemForm = true },
                            { editingItem = it; showItemForm = true },
                            { itemList.remove(it); saveItems(context, itemKey, itemList) },
                            { deliveryFamily = it; deliveryIsFlour = flour }, Modifier.padding(padding))
                        if (showItemForm) {
                            FoodItemForm(editingItem, { showItemForm = false }) { item ->
                                val index = itemList.indexOfFirst { it.id == item.id }
                                if (index >= 0) itemList[index] = item else itemList.add(item)
                                saveItems(context, itemKey, itemList); showItemForm = false
                            }
                        }
                        deliveryFamily?.let { family ->
                            FoodDeliveryForm(family, itemList.toList(), deliveryList.toList(), section, { deliveryFamily = null }) {
                                deliveryList.add(it); saveDeliveries(context, deliveryKey, deliveryList); deliveryFamily = null
                            }
                        }
                    }
                    "الرعاية الاجتماعية" -> {
                        SocialScreen(beneficiaries, socialCases, socialPayments, search, { search = it },
                            { section = "" }, { socialEditing = null; showSocialForm = true },
                            { socialEditing = it; showSocialForm = true }, { socialPaying = it }, Modifier.padding(padding))
                        if (showSocialForm) {
                            SocialCaseForm(beneficiaries, socialEditing, { showSocialForm = false }) { item ->
                                val index = socialCases.indexOfFirst { it.id == item.id }
                                if (index >= 0) socialCases[index] = item else socialCases.add(item)
                                saveSocialCases(context, socialCases); showSocialForm = false
                            }
                        }
                        socialPaying?.let { item ->
                            SocialPaymentForm(item, socialPaid(socialPayments, item.cardNumber), { socialPaying = null }) {
                                socialPayments.add(it); saveSocialPayments(context, socialPayments); socialPaying = null
                            }
                        }
                    }
                    "الديون والحسابات" -> {
                        DebtScreen(beneficiaries, debtRecords, debtPayments,
                            { debtFamily = it; debtAction = "add" },
                            { debtFamily = it; debtAction = "pay" }, { section = "" }, Modifier.padding(padding))
                        debtFamily?.let { family ->
                            if (debtAction == "add") DebtRecordForm(family, { debtFamily = null; debtAction = "" }) {
                                debtRecords.add(it); saveDebtRecords(context, debtRecords); debtFamily = null; debtAction = ""
                            }
                            if (debtAction == "pay") DebtPaymentForm(family, debtPaid(debtPayments, family.cardNumber),
                                debtTotal(debtRecords, family.cardNumber), { debtFamily = null; debtAction = "" }) {
                                debtPayments.add(it); saveDebtPayments(context, debtPayments); debtFamily = null; debtAction = ""
                            }
                        }
                    }
                    "التسليم" -> MovementScreen(foodDeliveries + flourDeliveries, { section = "" }, Modifier.padding(padding))
                    "كشف المتغيرات" -> ChangesScreen(beneficiaries, foodDeliveries + flourDeliveries, { section = "" }, Modifier.padding(padding))
                    "التقارير" -> ReportsScreen(beneficiaries.size, foodItems.size, flourItems.size,
                        foodDeliveries.size, flourDeliveries.size, socialCases.size, debtRecords.sumOf { it.amount },
                        { section = "" }, Modifier.padding(padding))
                    "قارئ PDF" -> PdfReaderScreen(
    onBack = { section = "" },
    modifier = Modifier.padding(padding)
)
                    "سجل التحركات" -> MovementScreen(foodDeliveries + flourDeliveries, { section = "" }, Modifier.padding(padding))
                    "حافظة الملفات" -> SimpleInfoScreen("حافظة الملفات", "سيتم ربط هذا القسم بملفات التقارير والمستندات المحفوظة.", { section = "" }, Modifier.padding(padding))
                    "الإعدادات" -> SettingsScreen(beneficiaries.size, { section = "" }, Modifier.padding(padding))
                    else -> HomeScreen(sections, section, { section = it; search = "" }, padding)
                }
            }
            if (showPersonForm) {
                BeneficiaryForm(editingPerson, { showPersonForm = false }) { person ->
                    val index = beneficiaries.indexOfFirst { it.id == person.id }
                    if (index >= 0) beneficiaries[index] = person else beneficiaries.add(person)
                    saveBeneficiaries(context, beneficiaries); showPersonForm = false
                }
            }
        }
    }
}

@Composable
private fun HomeScreen(sections: List<AppSection>, section: String, onOpen: (String) -> Unit, padding: PaddingValues) {
    Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
        Text("لوحة التحكم الرئيسية", fontSize = 21.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(14.dp))
        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFF126B53)),
            shape = RoundedCornerShape(18.dp)) {
            Column(Modifier.padding(20.dp)) {
                Text("مرحباً بك في وكيل بلس", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text("إدارة أعمالك التموينية من مكان واحد", color = Color.White)
            }
        }
        Spacer(Modifier.height(20.dp))
        Text("أقسام النظام", fontSize = 19.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        LazyVerticalGrid(columns = GridCells.Fixed(2), verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            gridItems(sections) { item ->
                Card(Modifier.fillMaxWidth().height(112.dp).clickable { onOpen(item.title) },
                    colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp)) {
                    Column(Modifier.fillMaxSize().padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center) {
                        Text(item.icon, fontSize = 28.sp)
                        Spacer(Modifier.height(8.dp))
                        Text(item.title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = item.color)
                    }
                }
            }
        }
    }
}

@Composable
private fun PageHeader(title: String, onBack: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(title, fontSize = 21.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        TextButton(onClick = onBack) { Text("الرئيسية") }
    }
}

@Composable
fun BeneficiariesScreen(
    beneficiaries: List<Beneficiary>, search: String, onSearch: (String) -> Unit,
    onBack: () -> Unit, onAdd: () -> Unit, onEdit: (Beneficiary) -> Unit,
    onDelete: (Beneficiary) -> Unit, modifier: Modifier = Modifier
) {
    Column(modifier.fillMaxSize().padding(14.dp)) {
        PageHeader("سجل المستفيدين", onBack)
        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFF126B53))) {
            Column(Modifier.padding(16.dp)) {
                Text("عدد المستفيدين", color = Color.White)
                Text(beneficiaries.size.toString(), color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(search, onSearch, Modifier.fillMaxWidth(), label = { Text("ابحث بالاسم أو رقم البطاقة") }, singleLine = true)
        Button(onAdd, Modifier.fillMaxWidth()) { Text("＋ إضافة مستفيد جديد") }
        val filtered = beneficiaries.filter { it.name.contains(search, true) || it.cardNumber.contains(search, true) || it.phone.contains(search, true) }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(filtered, key = { it.id }) { person ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text(person.name, fontWeight = FontWeight.Bold)
                        Text("العنوان: ${person.address}")
                        Text("الهاتف: ${person.phone}")
                        Text("عدد أفراد الأسرة: ${person.members}")
                        Text("رقم البطاقة: ${person.cardNumber}")
                        Text("رقم NFC: ${person.nfc}")
                        Row {
                            TextButton({ onEdit(person) }) { Text("تعديل") }
                            TextButton({ onDelete(person) }) { Text("حذف", color = Color.Red) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun QuotaScreen(
    title: String, beneficiaries: List<Beneficiary>, quotaItems: List<FoodItem>, deliveries: List<FoodDelivery>,
    search: String, onSearch: (String) -> Unit, onBack: () -> Unit, onAddItem: () -> Unit,
    onEditItem: (FoodItem) -> Unit, onDeleteItem: (FoodItem) -> Unit,
    onStartDelivery: (Beneficiary) -> Unit, modifier: Modifier = Modifier
) {
    val flour = title == "مادة الطحين"
    val color = if (flour) Color(0xFFD18B26) else Color(0xFF2878C7)
    Column(modifier.fillMaxSize().padding(14.dp)) {
        PageHeader(title, onBack)
        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = color)) {
            Column(Modifier.padding(14.dp)) {
                Text("سجل $title", color = Color.White)
                Text("عدد المواد: ${quotaItems.size}", color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.Bold)
                Text("عمليات التسليم: ${deliveries.size}", color = Color.White)
            }
        }
        Spacer(Modifier.height(8.dp))
        Button(onAddItem, Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = color)) { Text("＋ إضافة مادة جديدة") }
        Text("المواد المسجلة", fontWeight = FontWeight.Bold)
        if (quotaItems.isEmpty()) Text("لم تتم إضافة مواد بعد.")
        else LazyColumn(Modifier.heightIn(max = 175.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            items(quotaItems, key = { it.id }) { item ->
                Card(Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(item.name, fontWeight = FontWeight.Bold)
                            Text("استحقاق الفرد: ${quantityText(item.perPerson)} ${item.unit}")
                        }
                        TextButton({ onEditItem(item) }) { Text("تعديل") }
                        TextButton({ onDeleteItem(item) }) { Text("حذف", color = Color.Red) }
                    }
                }
            }
        }
        Divider(Modifier.padding(vertical = 8.dp))
        Text("سجل المستفيدين", fontWeight = FontWeight.Bold, fontSize = 17.sp)
        OutlinedTextField(search, onSearch, Modifier.fillMaxWidth(), label = { Text("ابحث بالاسم أو رقم البطاقة التموينية") }, singleLine = true)
        val matches = beneficiaries.filter { search.isNotBlank() && (it.name.contains(search, true) || it.cardNumber.contains(search, true)) }
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(matches, key = { it.id }) { family ->
                Card(Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(family.name, fontWeight = FontWeight.Bold)
                            Text("البطاقة: ${family.cardNumber}")
                            Text("أفراد الأسرة: ${family.members}")
                        }
                        Button(enabled = quotaItems.isNotEmpty(), onClick = { onStartDelivery(family) },
                            colors = ButtonDefaults.buttonColors(containerColor = color)) { Text("تسليم") }
                    }
                }
            }
        }
        if (search.isNotBlank() && matches.isEmpty()) Text("لم يتم العثور على عائلة مطابقة.")
    }
}

@Composable
fun FoodItemForm(item: FoodItem?, onDismiss: () -> Unit, onSave: (FoodItem) -> Unit) {
    var name by remember(item) { mutableStateOf(item?.name ?: "") }
    var unit by remember(item) { mutableStateOf(item?.unit ?: "كغم") }
    var amount by remember(item) { mutableStateOf(item?.perPerson?.let { quantityText(it) } ?: "") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(if (item == null) "إضافة مادة" else "تعديل المادة") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("اسم المادة") })
                OutlinedTextField(unit, { unit = it }, label = { Text("وحدة القياس") })
                OutlinedTextField(amount, { amount = it }, label = { Text("استحقاق الفرد الواحد") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
            }
        },
        confirmButton = {
            Button(enabled = name.isNotBlank() && unit.isNotBlank() && (amount.toDoubleOrNull() ?: 0.0) > 0,
                onClick = { onSave(FoodItem(item?.id ?: System.currentTimeMillis(), name.trim(), unit.trim(), amount.toDouble())) }) { Text("حفظ المادة") }
        },
        dismissButton = { TextButton(onDismiss) { Text("إلغاء") } })
}

@Composable
fun FoodDeliveryForm(
    family: Beneficiary, quotaItems: List<FoodItem>, deliveries: List<FoodDelivery>,
    title: String, onDismiss: () -> Unit, onSave: (FoodDelivery) -> Unit
) {
    val memberCount = family.members.toDoubleOrNull() ?: 0.0
    val quantities = remember(family.id, quotaItems, deliveries) {
        mutableStateMapOf<Long, String>().apply {
            quotaItems.forEach { item ->
                val remain = (item.perPerson * memberCount - deliveredAmount(deliveries, family.cardNumber, item.id)).coerceAtLeast(0.0)
                put(item.id, quantityText(remain))
            }
        }
    }
    val valid = quotaItems.isNotEmpty() && quotaItems.all { item ->
        val remain = (item.perPerson * memberCount - deliveredAmount(deliveries, family.cardNumber, item.id)).coerceAtLeast(0.0)
        val entered = quantities[item.id]?.toDoubleOrNull() ?: 0.0
        entered >= 0 && entered <= remain
    } && quotaItems.any { (quantities[it.id]?.toDoubleOrNull() ?: 0.0) > 0 }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("تسليم $title") },
        text = {
            Column {
                Text(family.name, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text("رقم البطاقة: ${family.cardNumber}")
                Text("عدد أفراد الأسرة: ${family.members}")
                Spacer(Modifier.height(8.dp))
                LazyColumn(Modifier.heightIn(max = 390.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(quotaItems, key = { it.id }) { item ->
                        val entitled = item.perPerson * memberCount
                        val already = deliveredAmount(deliveries, family.cardNumber, item.id)
                        val remaining = (entitled - already).coerceAtLeast(0.0)
                        Card {
                            Column(Modifier.padding(10.dp)) {
                                Text(item.name, fontWeight = FontWeight.Bold)
                                Text("الاستحقاق: ${quantityText(entitled)} ${item.unit}")
                                Text("المسلّم سابقاً: ${quantityText(already)} ${item.unit}")
                                Text("المتبقي: ${quantityText(remaining)} ${item.unit}", color = Color(0xFF168B68))
                                OutlinedTextField(quantities[item.id] ?: "0", { quantities[item.id] = it },
                                    Modifier.fillMaxWidth(), label = { Text("الكمية المسلّمة الآن (${item.unit})") }, singleLine = true)
                                val entered = quantities[item.id]?.toDoubleOrNull()
                                if (entered != null && entered > remaining) Text("الكمية أكبر من المتبقي", color = Color.Red)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(enabled = valid, onClick = {
                val lines = quotaItems.mapNotNull { item ->
                    val q = quantities[item.id]?.toDoubleOrNull() ?: 0.0
                    if (q > 0) DeliveryLine(item.id, item.name, q) else null
                }
                onSave(FoodDelivery(System.currentTimeMillis(), family.cardNumber, family.name, nowText(), lines))
            }) { Text("حفظ عملية التسليم") }
        },
        dismissButton = { TextButton(onDismiss) { Text("إلغاء") } })
}

@Composable
fun BeneficiaryForm(person: Beneficiary?, onDismiss: () -> Unit, onSave: (Beneficiary) -> Unit) {
    var name by remember(person) { mutableStateOf(person?.name ?: "") }
    var address by remember(person) { mutableStateOf(person?.address ?: "") }
    var phone by remember(person) { mutableStateOf(person?.phone ?: "") }
    var members by remember(person) { mutableStateOf(person?.members ?: "") }
    var card by remember(person) { mutableStateOf(person?.cardNumber ?: "") }
    var nfc by remember(person) { mutableStateOf(person?.nfc ?: "") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(if (person == null) "إضافة مستفيد جديد" else "تعديل بيانات المستفيد") },
        text = {
            LazyColumn(Modifier.heightIn(max = 430.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item { OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), label = { Text("الاسم الكامل") }) }
                item { OutlinedTextField(address, { address = it }, Modifier.fillMaxWidth(), label = { Text("العنوان") }) }
                item { OutlinedTextField(phone, { phone = it }, Modifier.fillMaxWidth(), label = { Text("رقم واتساب / الهاتف") }) }
                item { OutlinedTextField(members, { members = it }, Modifier.fillMaxWidth(), label = { Text("عدد أفراد الأسرة") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)) }
                item { OutlinedTextField(card, { card = it }, Modifier.fillMaxWidth(), label = { Text("رقم البطاقة التموينية") }) }
                item { OutlinedTextField(nfc, { nfc = it }, Modifier.fillMaxWidth(), label = { Text("رقم بطاقة NFC") }) }
            }
        },
        confirmButton = {
            Button(enabled = name.isNotBlank() && card.isNotBlank(), onClick = {
                onSave(Beneficiary(person?.id ?: System.currentTimeMillis(), name.trim(), address.trim(), phone.trim(), members.trim(), card.trim(), nfc.trim()))
            }) { Text("حفظ المستفيد") }
        },
        dismissButton = { TextButton(onDismiss) { Text("إلغاء") } })
}

@Composable
fun SocialScreen(
    beneficiaries: List<Beneficiary>, cases: List<SocialCase>, payments: List<SocialPayment>,
    search: String, onSearch: (String) -> Unit, onBack: () -> Unit, onAdd: () -> Unit,
    onEdit: (SocialCase) -> Unit, onPay: (SocialCase) -> Unit, modifier: Modifier = Modifier
) {
    val totalEntitlements = cases.sumOf { it.monthlyAmount }
    val totalPaid = cases.sumOf { socialPaid(payments, it.cardNumber) }
    Column(modifier.fillMaxSize().padding(14.dp)) {
        PageHeader("الرعاية الاجتماعية", onBack)
        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFF8656B8))) {
            Column(Modifier.padding(14.dp)) {
                Text("عدد العوائل المسجلة: ${cases.size}", color = Color.White)
                Text("مجموع الاستحقاقات: ${quantityText(totalEntitlements)}", color = Color.White)
                Text("مجموع المدفوعات المسجلة: ${quantityText(totalPaid)}", color = Color.White)
            }
        }
        Spacer(Modifier.height(8.dp))
        Button(onAdd, Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8656B8))) {
            Text("＋ إضافة مستفيد للرعاية الاجتماعية")
        }
        OutlinedTextField(search, onSearch, Modifier.fillMaxWidth(), label = { Text("ابحث بالاسم أو رقم البطاقة التموينية") }, singleLine = true)
        val filtered = cases.filter { it.familyName.contains(search, true) || it.cardNumber.contains(search, true) }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(filtered, key = { it.id }) { social ->
                val paid = socialPaid(payments, social.cardNumber)
                val remaining = (social.monthlyAmount - paid).coerceAtLeast(0.0)
                val family = beneficiaries.find { it.cardNumber == social.cardNumber }
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text(social.familyName, fontWeight = FontWeight.Bold)
                        Text("رقم البطاقة: ${social.cardNumber}")
                        Text("عدد أفراد الأسرة: ${family?.members ?: "غير متوفر"}")
                        Text("الاستحقاق الشهري: ${quantityText(social.monthlyAmount)}")
                        Text("المبلغ المدفوع: ${quantityText(paid)}")
                        Text("المتبقي: ${quantityText(remaining)}", color = if (remaining > 0) Color(0xFFB36B12) else Color(0xFF168B68))
                        Row {
                            TextButton({ onEdit(social) }) { Text("تعديل الاستحقاق") }
                            TextButton(enabled = remaining > 0, onClick = { onPay(social) }) { Text("تسجيل دفعة") }
                        }
                        val history = payments.filter { it.cardNumber == social.cardNumber }
                        if (history.isNotEmpty()) {
                            Divider()
                            Text("سجل المدفوعات", fontWeight = FontWeight.Bold)
                            history.sortedByDescending { it.id }.forEach { Text("${it.date} • ${quantityText(it.amount)}") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SocialPaymentForm(social: SocialCase, paid: Double, onDismiss: () -> Unit, onSave: (SocialPayment) -> Unit) {
    var amount by remember(social.id) { mutableStateOf("") }
    val remaining = (social.monthlyAmount - paid).coerceAtLeast(0.0)
    val entered = amount.toDoubleOrNull() ?: 0.0
    AlertDialog(onDismissRequest = onDismiss, title = { Text("تسجيل دفعة للرعاية الاجتماعية") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(social.familyName, fontWeight = FontWeight.Bold)
                Text("رقم البطاقة: ${social.cardNumber}")
                Text("الاستحقاق الشهري: ${quantityText(social.monthlyAmount)}")
                Text("المدفوع سابقاً: ${quantityText(paid)}")
                Text("المتبقي: ${quantityText(remaining)}")
                OutlinedTextField(amount, { amount = it }, Modifier.fillMaxWidth(), label = { Text("المبلغ المدفوع الآن") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                if (entered > remaining) Text("المبلغ أكبر من المتبقي", color = Color.Red)
            }
        },
        confirmButton = {
            Button(enabled = entered > 0 && entered <= remaining, onClick = {
                onSave(SocialPayment(System.currentTimeMillis(), social.cardNumber, social.familyName, nowText(), entered))
            }) { Text("حفظ الدفعة") }
        },
        dismissButton = { TextButton(onDismiss) { Text("إلغاء") } })
}

@Composable
fun SocialCaseForm(beneficiaries: List<Beneficiary>, existing: SocialCase?, onDismiss: () -> Unit, onSave: (SocialCase) -> Unit) {
    var card by remember(existing) { mutableStateOf(existing?.cardNumber ?: "") }
    var familyName by remember(existing) { mutableStateOf(existing?.familyName ?: "") }
    var amount by remember(existing) { mutableStateOf(existing?.monthlyAmount?.let { quantityText(it) } ?: "") }
    val match = beneficiaries.find { it.cardNumber == card.trim() }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(if (existing == null) "إضافة مستفيد للرعاية الاجتماعية" else "تعديل الاستحقاق الاجتماعي") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(card, { value ->
                    card = value
                    beneficiaries.find { it.cardNumber == value.trim() }?.let { familyName = it.name }
                }, Modifier.fillMaxWidth(), label = { Text("رقم البطاقة التموينية") }, singleLine = true)
                if (match != null) Text("العائلة المسجلة: ${match.name}", color = Color(0xFF168B68))
                OutlinedTextField(familyName, { familyName = it }, Modifier.fillMaxWidth(), label = { Text("اسم رب الأسرة") }, singleLine = true)
                OutlinedTextField(amount, { amount = it }, Modifier.fillMaxWidth(), label = { Text("الاستحقاق الشهري") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
            }
        },
        confirmButton = {
            Button(enabled = card.isNotBlank() && familyName.isNotBlank() && (amount.toDoubleOrNull() ?: 0.0) > 0,
                onClick = { onSave(SocialCase(existing?.id ?: System.currentTimeMillis(), card.trim(), familyName.trim(), amount.toDouble())) }) {
                Text("حفظ البيانات")
            }
        },
        dismissButton = { TextButton(onDismiss) { Text("إلغاء") } })
}

@Composable
fun DebtScreen(
    beneficiaries: List<Beneficiary>, records: List<DebtRecord>, payments: List<DebtPayment>,
    onAddDebt: (Beneficiary) -> Unit, onPayDebt: (Beneficiary) -> Unit, onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val totalDebt = records.sumOf { it.amount }
    val totalPaid = payments.sumOf { it.amount }
    Column(modifier.fillMaxSize().padding(14.dp)) {
        PageHeader("الديون والحسابات", onBack)
        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFF2878C7))) {
            Column(Modifier.padding(16.dp)) {
                Text("إجمالي الديون: ${quantityText(totalDebt)}", color = Color.White)
                Text("إجمالي المدفوع: ${quantityText(totalPaid)}", color = Color.White)
                Text("المتبقي: ${quantityText((totalDebt - totalPaid).coerceAtLeast(0.0))}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            }
        }
        Spacer(Modifier.height(12.dp))
        Text("حسابات العوائل", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(beneficiaries, key = { it.id }) { family ->
                val debt = debtTotal(records, family.cardNumber)
                val paid = debtPaid(payments, family.cardNumber)
                val balance = (debt - paid).coerceAtLeast(0.0)
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text(family.name, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                        Text("رقم البطاقة: ${family.cardNumber}")
                        Text("إجمالي الدين: ${quantityText(debt)}")
                        Text("المدفوع: ${quantityText(paid)}")
                        Text("المتبقي: ${quantityText(balance)}", color = if (balance > 0) Color.Red else Color(0xFF168B68))
                        Row {
                            TextButton({ onAddDebt(family) }) { Text("＋ تسجيل دين") }
                            TextButton(enabled = balance > 0, onClick = { onPayDebt(family) }) { Text("تسجيل تسديد") }
                        }
                        val history = records.filter { it.cardNumber == family.cardNumber }
                        if (history.isNotEmpty()) {
                            Divider()
                            Text("سجل الديون", fontWeight = FontWeight.Bold)
                            history.sortedByDescending { it.id }.forEach { Text("${it.date} • ${it.description} • ${quantityText(it.amount)}") }
                        }
                        val paymentHistory = payments.filter { it.cardNumber == family.cardNumber }
                        if (paymentHistory.isNotEmpty()) {
                            Divider()
                            Text("سجل التسديدات", fontWeight = FontWeight.Bold)
                            paymentHistory.sortedByDescending { it.id }.forEach { Text("${it.date} • ${quantityText(it.amount)} • ${it.note}") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DebtRecordForm(family: Beneficiary, onDismiss: () -> Unit, onSave: (DebtRecord) -> Unit) {
    var description by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    val entered = amount.toDoubleOrNull() ?: 0.0
    AlertDialog(onDismissRequest = onDismiss, title = { Text("تسجيل دين جديد") },
        text = {
            Column {
                Text("العائلة: ${family.name}")
                Text("رقم البطاقة: ${family.cardNumber}")
                OutlinedTextField(description, { description = it }, Modifier.fillMaxWidth(), label = { Text("وصف الدين") })
                OutlinedTextField(amount, { amount = it }, Modifier.fillMaxWidth(), label = { Text("المبلغ") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
            }
        },
        confirmButton = {
            TextButton(enabled = description.isNotBlank() && entered > 0, onClick = {
                onSave(DebtRecord(System.currentTimeMillis(), family.cardNumber, family.name, description.trim(), nowText(), entered))
            }) { Text("حفظ") }
        },
        dismissButton = { TextButton(onDismiss) { Text("إلغاء") } })
}

@Composable
fun DebtPaymentForm(family: Beneficiary, paid: Double, debt: Double, onDismiss: () -> Unit, onSave: (DebtPayment) -> Unit) {
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    val remaining = (debt - paid).coerceAtLeast(0.0)
    val entered = amount.toDoubleOrNull() ?: 0.0
    AlertDialog(onDismissRequest = onDismiss, title = { Text("تسجيل تسديد دين") },
        text = {
            Column {
                Text("العائلة: ${family.name}")
                Text("المتبقي: ${quantityText(remaining)}")
                OutlinedTextField(amount, { amount = it }, Modifier.fillMaxWidth(), label = { Text("مبلغ التسديد") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                OutlinedTextField(note, { note = it }, Modifier.fillMaxWidth(), label = { Text("ملاحظة (اختياري)") })
            }
        },
        confirmButton = {
            TextButton(enabled = entered > 0 && entered <= remaining, onClick = {
                onSave(DebtPayment(System.currentTimeMillis(), family.cardNumber, family.name, nowText(), entered, note.trim()))
            }) { Text("حفظ التسديد") }
        },
        dismissButton = { TextButton(onDismiss) { Text("إلغاء") } })
}

@Composable
private fun MovementScreen(deliveries: List<FoodDelivery>, onBack: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize().padding(14.dp)) {
        PageHeader("سجل التحركات والتسليم", onBack)
        Text("عدد عمليات التسليم المسجلة: ${deliveries.size}", fontWeight = FontWeight.Bold)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(deliveries.sortedByDescending { it.id }, key = { it.id }) { d ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text(d.familyName, fontWeight = FontWeight.Bold)
                        Text("رقم البطاقة: ${d.cardNumber}")
                        Text("التاريخ: ${d.date}")
                        d.lines.forEach { Text("${it.itemName}: ${quantityText(it.quantity)}") }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChangesScreen(beneficiaries: List<Beneficiary>, deliveries: List<FoodDelivery>, onBack: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize().padding(14.dp)) {
        PageHeader("كشف المتغيرات", onBack)
        Text("ملخص بيانات السجل الحالي", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        SummaryCard("إجمالي العوائل المسجلة", beneficiaries.size.toString())
        SummaryCard("العوائل التي لديها عمليات تسليم", deliveries.map { it.cardNumber }.distinct().size.toString())
        SummaryCard("عدد عمليات التسليم", deliveries.size.toString())
        Text("ملاحظة: مقارنة قوائم شهرين واستخراج العوائل الوافدة والمغادرة تحتاج إلى استيراد ملفات القوائم في مرحلة قارئ PDF.", color = Color.DarkGray)
    }
}

@Composable
private fun ReportsScreen(
    people: Int, foodItems: Int, flourItems: Int, foodDeliveries: Int,
    flourDeliveries: Int, socialCases: Int, debts: Double, onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier.fillMaxSize().padding(14.dp)) {
        PageHeader("التقارير", onBack)
        SummaryCard("عدد المستفيدين", people.toString())
        SummaryCard("مواد الحصة الغذائية", foodItems.toString())
        SummaryCard("مواد الطحين", flourItems.toString())
        SummaryCard("عمليات تسليم الحصة", foodDeliveries.toString())
        SummaryCard("عمليات تسليم الطحين", flourDeliveries.toString())
        SummaryCard("حالات الرعاية الاجتماعية", socialCases.toString())
        SummaryCard("إجمالي الديون المسجلة", quantityText(debts))
        Text("هذه إحصاءات مباشرة من البيانات المحفوظة على الجهاز.", color = Color.DarkGray)
    }
}

@Composable
private fun SummaryCard(label: String, value: String) {
    Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(label, Modifier.weight(1f), fontWeight = FontWeight.Medium)
            Text(value, fontWeight = FontWeight.Bold, color = Color(0xFF126B53), fontSize = 18.sp)
        }
    }
}

@Composable
private fun SimpleInfoScreen(title: String, message: String, onBack: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize().padding(14.dp)) {
        PageHeader(title, onBack)
        
Card(Modifier.fillMaxWidth()) {
    Text(message, Modifier.padding(16.dp))
    }
}
}

@Composable
private fun SettingsScreen(
    beneficiariesCount: Int,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier.fillMaxSize().padding(14.dp)) {
        PageHeader("الإعدادات", onBack)

        SummaryCard(
            "عدد المستفيدين المسجلين",
            beneficiariesCount.toString()
        )

        SummaryCard(
            "اسم التطبيق",
            "وكيل بلس"
        )

        SummaryCard(
            "حفظ البيانات",
            "يتم الحفظ تلقائياً على الجهاز"
        )

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    "حول التطبيق",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Spacer(Modifier.height(8.dp))
                Text("وكيل بلس - نظام إدارة الوكيل التمويني")
                Text("الإصدار 1.0")
            }
        }
    }
}
@Composable
private fun PdfReaderScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var pdfText by remember { mutableStateOf("") }
    var fileName by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            isLoading = true
            errorMessage = ""
            fileName = uri.lastPathSegment ?: "ملف PDF"

            scope.launch {
                try {
                    val extractedText = withContext(Dispatchers.IO) {
                        PDFBoxResourceLoader.init(context)

                        val input = context.contentResolver.openInputStream(uri)
                            ?: throw Exception("تعذر فتح الملف")

                        input.use { stream ->
                            PDDocument.load(stream).use { document ->
                                PDFTextStripper().getText(document)
                            }
                        }
                    }

                    if (extractedText.isBlank()) {
                        errorMessage = "لم يتم العثور على نص داخل الملف. قد يكون PDF عبارة عن صور ممسوحة ضوئياً."
                    } else {
                        pdfText = extractedText

                        prefs(context).edit()
                            .putString("wakilplus_last_pdf_text", extractedText)
                            .putString("wakilplus_last_pdf_name", fileName)
                            .apply()
                    }
                } catch (e: Exception) {
                    errorMessage = "حدث خطأ أثناء قراءة الملف: ${e.message ?: "خطأ غير معروف"}"
                } finally {
                    isLoading = false
                }
            }
        }
    }

    Column(
        modifier.fillMaxSize().padding(14.dp)
    ) {
        PageHeader("قارئ ملفات PDF", onBack)

        Card(
            Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF126B53)
            )
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    "استيراد القوائم التموينية",
                    color = Color.White,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(6.dp))

                Text(
                    "اختر ملف PDF لاستخراج النصوص الموجودة بداخله.",
                    color = Color.White
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        Button(
            onClick = {
                filePicker.launch(arrayOf("application/pdf"))
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        ) {
            Text("📂 اختيار ملف PDF من الهاتف")
        }

        if (isLoading) {
            Spacer(Modifier.height(12.dp))
            CircularProgressIndicator()
            Text("جارٍ قراءة الملف...")
        }

        if (errorMessage.isNotBlank()) {
            Spacer(Modifier.height(10.dp))
            Text(
                errorMessage,
                color = Color.Red
            )
        }

        if (pdfText.isNotBlank()) {
            Spacer(Modifier.height(12.dp))

            Text(
                "الملف: $fileName",
                fontWeight = FontWeight.Bold
            )

            Text(
                "تم استخراج النص بنجاح",
                color = Color(0xFF168B68),
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(8.dp))

            Card(
                Modifier.fillMaxWidth().weight(1f)
            ) {
                Text(
                    text = pdfText,
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(12.dp),
                    fontSize = 14.sp
                )
            }
        } else if (!isLoading) {
            Spacer(Modifier.height(12.dp))
            Text(
                "لم يتم اختيار أي ملف بعد.",
                color = Color.DarkGray
            )
        }
    }
}
