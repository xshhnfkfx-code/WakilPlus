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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { WakilPlusApp() }
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

private const val BENEFICIARIES_KEY = "wakilplus_beneficiaries"
private const val FOOD_ITEMS_KEY = "wakilplus_food_items"
private const val FOOD_DELIVERIES_KEY = "wakilplus_food_deliveries"

fun appPreferences(context: Context) =
    context.getSharedPreferences("WakilPlusData", Context.MODE_PRIVATE)

fun loadBeneficiaries(context: Context): List<Beneficiary> {
    return try {
        val array = JSONArray(
            appPreferences(context).getString(BENEFICIARIES_KEY, "[]")
        )
        val result = mutableListOf<Beneficiary>()
        for (i in 0 until array.length()) {
            val item = array.getJSONObject(i)
            result.add(
                Beneficiary(
                    item.optLong("id"),
                    item.optString("name"),
                    item.optString("address"),
                    item.optString("phone"),
                    item.optString("members"),
                    item.optString("cardNumber"),
                    item.optString("nfc")
                )
            )
        }
        result
    } catch (_: Exception) {
        emptyList()
    }
}

fun saveBeneficiaries(context: Context, list: List<Beneficiary>) {
    val array = JSONArray()
    list.forEach {
        val obj = JSONObject()
        obj.put("id", it.id)
        obj.put("name", it.name)
        obj.put("address", it.address)
        obj.put("phone", it.phone)
        obj.put("members", it.members)
        obj.put("cardNumber", it.cardNumber)
        obj.put("nfc", it.nfc)
        array.put(obj)
    }
    appPreferences(context).edit()
        .putString(BENEFICIARIES_KEY, array.toString()).apply()
}

fun loadFoodItems(context: Context): List<FoodItem> {
    return try {
        val array = JSONArray(
            appPreferences(context).getString(FOOD_ITEMS_KEY, "[]")
        )
        val result = mutableListOf<FoodItem>()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            result.add(
                FoodItem(
                    obj.optLong("id"),
                    obj.optString("name"),
                    obj.optString("unit"),
                    obj.optDouble("perPerson")
                )
            )
        }
        result
    } catch (_: Exception) {
        emptyList()
    }
}

fun saveFoodItems(context: Context, list: List<FoodItem>) {
    val array = JSONArray()
    list.forEach {
        val obj = JSONObject()
        obj.put("id", it.id)
        obj.put("name", it.name)
        obj.put("unit", it.unit)
        obj.put("perPerson", it.perPerson)
        array.put(obj)
    }
    appPreferences(context).edit()
        .putString(FOOD_ITEMS_KEY, array.toString()).apply()
}

fun loadFoodDeliveries(context: Context): List<FoodDelivery> {
    return try {
        val array = JSONArray(
            appPreferences(context).getString(FOOD_DELIVERIES_KEY, "[]")
        )
        val result = mutableListOf<FoodDelivery>()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val linesArray = obj.optJSONArray("lines") ?: JSONArray()
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
                    obj.optLong("id"),
                    obj.optString("cardNumber"),
                    obj.optString("familyName"),
                    obj.optString("date"),
                    lines
                )
            )
        }
        result
    } catch (_: Exception) {
        emptyList()
    }
}

fun saveFoodDeliveries(context: Context, list: List<FoodDelivery>) {
    val array = JSONArray()
    list.forEach { delivery ->
        val obj = JSONObject()
        obj.put("id", delivery.id)
        obj.put("cardNumber", delivery.cardNumber)
        obj.put("familyName", delivery.familyName)
        obj.put("date", delivery.date)

        val lines = JSONArray()
        delivery.lines.forEach { line ->
            val lineObj = JSONObject()
            lineObj.put("itemId", line.itemId)
            lineObj.put("itemName", line.itemName)
            lineObj.put("quantity", line.quantity)
            lines.put(lineObj)
        }
        obj.put("lines", lines)
        array.put(obj)
    }
    appPreferences(context).edit()
        .putString(FOOD_DELIVERIES_KEY, array.toString()).apply()
}

fun deliveredAmount(
    deliveries: List<FoodDelivery>,
    cardNumber: String,
    itemId: Long
): Double {
    return deliveries
        .filter { it.cardNumber == cardNumber }
        .flatMap { it.lines }
        .filter { it.itemId == itemId }
        .sumOf { it.quantity }
}

fun formatQuantity(value: Double): String {
    return if (value % 1.0 == 0.0) {
        value.toLong().toString()
    } else {
        String.format(Locale.US, "%.2f", value)
    }
}

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
            addAll(loadFoodItems(context))
        }
    }

    val deliveries = remember {
        mutableStateListOf<FoodDelivery>().apply {
            addAll(loadFoodDeliveries(context))
        }
    }

    var selectedSection by remember { mutableStateOf("") }
    var search by remember { mutableStateOf("") }
    var rationSearch by remember { mutableStateOf("") }
    var showBeneficiaryForm by remember { mutableStateOf(false) }
    var editingBeneficiary by remember { mutableStateOf<Beneficiary?>(null) }
    var showFoodForm by remember { mutableStateOf(false) }
    var editingFood by remember { mutableStateOf<FoodItem?>(null) }
    var deliveryFamily by remember { mutableStateOf<Beneficiary?>(null) }

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
                        Text("نظام الوكيل التمويني", color = Color.White)
                    }
                }
            ) { padding ->

                when (selectedSection) {
                    "المستفيدون" -> {
                        BeneficiariesScreen(
                            beneficiaries = beneficiaries,
                            search = search,
                            onSearch = { search = it },
                            onBack = { selectedSection = "" },
                            onAdd = {
                                editingBeneficiary = null
                                showBeneficiaryForm = true
                            },
                            onEdit = {
                                editingBeneficiary = it
                                showBeneficiaryForm = true
                            },
                            onDelete = {
                                beneficiaries.remove(it)
                                saveBeneficiaries(context, beneficiaries)
                            },
                            modifier = Modifier.padding(padding)
                        )
                    }

                    "الحصة الغذائية" -> {
                        FoodSectionScreen(
                            beneficiaries = beneficiaries,
                            foodItems = foodItems,
                            deliveries = deliveries,
                            search = rationSearch,
                            onSearch = { rationSearch = it },
                            onBack = { selectedSection = "" },
                            onAddFood = {
                                editingFood = null
                                showFoodForm = true
                            },
                            onEditFood = {
                                editingFood = it
                                showFoodForm = true
                            },
                            onDeleteFood = {
                                foodItems.remove(it)
                                saveFoodItems(context, foodItems)
                            },
                            onStartDelivery = {
                                deliveryFamily = it
                            },
                            modifier = Modifier.padding(padding)
                        )
                    }

                    else -> {
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
                                            )
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
            }

            if (showBeneficiaryForm) {
                BeneficiaryForm(
                    person = editingBeneficiary,
                    onDismiss = { showBeneficiaryForm = false },
                    onSave = { person ->
                        val index = beneficiaries.indexOfFirst {
                            it.id == person.id
                        }
                        if (index >= 0) beneficiaries[index] = person
                        else beneficiaries.add(person)

                        saveBeneficiaries(context, beneficiaries)
                        showBeneficiaryForm = false
                    }
                )
            }

            if (showFoodForm) {
                FoodItemForm(
                    item = editingFood,
                    onDismiss = { showFoodForm = false },
                    onSave = { food ->
                        val index = foodItems.indexOfFirst {
                            it.id == food.id
                        }
                        if (index >= 0) foodItems[index] = food
                        else foodItems.add(food)

                        saveFoodItems(context, foodItems)
                        showFoodForm = false
                    }
                )
            }

            deliveryFamily?.let { family ->
                FoodDeliveryForm(
                    family = family,
                    foodItems = foodItems.toList(),
                    deliveries = deliveries.toList(),
                    onDismiss = { deliveryFamily = null },
                    onSave = { delivery ->
                        deliveries.add(delivery)
                        saveFoodDeliveries(context, deliveries)
                        deliveryFamily = null
                    }
                )
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
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(14.dp)
    ) {
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
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF126B53)
            )
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
            value = search,
            onValueChange = onSearch,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("ابحث بالاسم أو رقم البطاقة") },
            singleLine = true
        )
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = onAdd,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("＋ إضافة مستفيد جديد")
        }
        Spacer(Modifier.height(8.dp))

        val filtered = beneficiaries.filter {
            it.name.contains(search, true) ||
                it.cardNumber.contains(search, true) ||
                it.phone.contains(search, true)
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(9.dp)) {
            items(filtered, key = { it.id }) { person ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(13.dp)) {
                        Text(
                            person.name,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF126B53)
                        )
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
fun FoodSectionScreen(
    beneficiaries: List<Beneficiary>,
    foodItems: List<FoodItem>,
    deliveries: List<FoodDelivery>,
    search: String,
    onSearch: (String) -> Unit,
    onBack: () -> Unit,
    onAddFood: () -> Unit,
    onEditFood: (FoodItem) -> Unit,
    onDeleteFood: (FoodItem) -> Unit,
    onStartDelivery: (Beneficiary) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "الحصة الغذائية",
                fontSize = 21.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onBack) { Text("الرئيسية") }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF2878C7)
            )
        ) {
            Column(Modifier.padding(14.dp)) {
                Text("مواد الحصة الغذائية", color = Color.White)
                Text(
                    "عدد المواد: ${foodItems.size}",
                    color = Color.White,
                    fontSize = 23.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "عمليات التسليم: ${deliveries.size}",
                    color = Color.White
                )
            }
        }

        Spacer(Modifier.height(10.dp))
        Button(
            onClick = onAddFood,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("＋ إضافة مادة غذائية")
        }

        Text(
            "المواد المسجلة",
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
        )

        if (foodItems.isEmpty()) {
            Text(
                "لم تتم إضافة مواد بعد. أضف المواد وحدد استحقاق الفرد الواحد.",
                modifier = Modifier.padding(vertical = 10.dp)
            )
        } else {
            LazyColumn(
                modifier = Modifier.heightIn(max = 220.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(foodItems, key = { it.id }) { food ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(food.name, fontWeight = FontWeight.Bold)
                                Text(
                                    "استحقاق الفرد: ${formatQuantity(food.perPerson)} ${food.unit}"
                                )
                            }
                            TextButton(onClick = { onEditFood(food) }) {
                                Text("تعديل")
                            }
                            TextButton(onClick = { onDeleteFood(food) }) {
                                Text("حذف", color = Color.Red)
                            }
                        }
                    }
                }
            }
        }

        Divider(Modifier.padding(vertical = 10.dp))

        Text(
            "تسجيل تسليم عائلة",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )

        OutlinedTextField(
            value = search,
            onValueChange = onSearch,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("ابحث برقم البطاقة التموينية أو الاسم") },
            singleLine = true
        )

        val matches = beneficiaries.filter {
            search.isNotBlank() && (
                it.cardNumber.contains(search, true) ||
                    it.name.contains(search, true)
                )
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(matches, key = { it.id }) { family ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(family.name, fontWeight = FontWeight.Bold)
                            Text("البطاقة: ${family.cardNumber}")
                            Text("أفراد الأسرة: ${family.members}")
                        }
                        Button(
                            enabled = foodItems.isNotEmpty(),
                            onClick = { onStartDelivery(family) }
                        ) {
                            Text("تسليم")
                        }
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
    onDismiss: () -> Unit,
    onSave: (FoodItem) -> Unit
) {
    var name by remember(item) { mutableStateOf(item?.name ?: "") }
    var unit by remember(item) { mutableStateOf(item?.unit ?: "كغم") }
    var perPerson by remember(item) {
        mutableStateOf(
            item?.perPerson?.let { formatQuantity(it) } ?: ""
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (item == null) "إضافة مادة غذائية" else "تعديل المادة")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("اسم المادة") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = unit,
                    onValueChange = { unit = it },
                    label = { Text("وحدة القياس (كغم، لتر، علبة...)") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = perPerson,
                    onValueChange = { perPerson = it },
                    label = { Text("استحقاق الفرد الواحد") },
                    singleLine = true
                )
                Text(
                    "مثال: إذا كان استحقاق الفرد 1 كغم، فالعائلة من 5 أفراد تستحق 5 كغم."
                )
            }
        },
        confirmButton = {
            Button(
                enabled = name.isNotBlank() &&
                    unit.isNotBlank() &&
                    (perPerson.toDoubleOrNull() ?: 0.0) > 0.0,
                onClick = {
                    onSave(
                        FoodItem(
                            id = item?.id ?: System.currentTimeMillis(),
                            name = name.trim(),
                            unit = unit.trim(),
                            perPerson = perPerson.toDouble()
                        )
                    )
                }
            ) {
                Text("حفظ المادة")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}

@Composable
fun FoodDeliveryForm(
    family: Beneficiary,
    foodItems: List<FoodItem>,
    deliveries: List<FoodDelivery>,
    onDismiss: () -> Unit,
    onSave: (FoodDelivery) -> Unit
) {
    val memberCount = family.members.toDoubleOrNull() ?: 0.0

    val quantities = remember(family.id, foodItems, deliveries) {
        mutableStateMapOf<Long, String>().apply {
            foodItems.forEach { food ->
                val entitled = food.perPerson * memberCount
                val alreadyDelivered = deliveredAmount(
                    deliveries,
                    family.cardNumber,
                    food.id
                )
                val remaining = (entitled - alreadyDelivered).coerceAtLeast(0.0)
                put(food.id, formatQuantity(remaining))
            }
        }
    }

    val canSave = foodItems.isNotEmpty() &&
        foodItems.all { food ->
            val entitled = food.perPerson * memberCount
            val already = deliveredAmount(
                deliveries,
                family.cardNumber,
                food.id
            )
            val remaining = (entitled - already).coerceAtLeast(0.0)
            val entered = quantities[food.id]?.toDoubleOrNull() ?: 0.0
            entered >= 0.0 && entered <= remaining
        } &&
        foodItems.any {
            (quantities[it.id]?.toDoubleOrNull() ?: 0.0) > 0.0
        }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تسليم الحصة الغذائية") },
        text = {
            Column {
                Text(
                    family.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Text("رقم البطاقة: ${family.cardNumber}")
                Text("عدد أفراد الأسرة: ${family.members}")
                Spacer(Modifier.height(10.dp))

                LazyColumn(
                    modifier = Modifier.heightIn(max = 390.dp),
                    verticalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    items(foodItems, key = { it.id }) { food ->
                        val entitlement = food.perPerson * memberCount
                        val already = deliveredAmount(
                            deliveries,
                            family.cardNumber,
                            food.id
                        )
                        val remaining = (entitlement - already).coerceAtLeast(0.0)

                        Card {
                            Column(Modifier.padding(10.dp)) {
                                Text(
                                    food.name,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "الاستحقاق الكلي: ${formatQuantity(entitlement)} ${food.unit}"
                                )
                                Text(
                                    "المسلّم سابقاً: ${formatQuantity(already)} ${food.unit}"
                                )
                                Text(
                                    "المتبقي: ${formatQuantity(remaining)} ${food.unit}",
                                    color = Color(0xFF168B68)
                                )
                                OutlinedTextField(
                                    value = quantities[food.id] ?: "0",
                                    onValueChange = {
                                        quantities[food.id] = it
                                    },
                                    label = {
                                        Text("الكمية المسلّمة الآن (${food.unit})")
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                                val entered =
                                    quantities[food.id]?.toDoubleOrNull()
                                if (entered != null && entered > remaining) {
                                    Text(
                                        "الكمية أكبر من المتبقي",
                                        color = Color.Red
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = canSave,
                onClick = {
                    val lines = foodItems.mapNotNull { food ->
                        val amount =
                            quantities[food.id]?.toDoubleOrNull() ?: 0.0
                        if (amount > 0.0) {
                            DeliveryLine(food.id, food.name, amount)
                        } else null
                    }

                    val date = SimpleDateFormat(
                        "yyyy/MM/dd - HH:mm",
                        Locale.getDefault()
                    ).format(Date())

                    onSave(
                        FoodDelivery(
                            id = System.currentTimeMillis(),
                            cardNumber = family.cardNumber,
                            familyName = family.name,
                            date = date,
                            lines = lines
                        )
                    )
                }
            ) {
                Text("حفظ عملية التسليم")
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
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}
