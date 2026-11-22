package com.chonkies.app
import androidx.compose.animation.core.*
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.delay
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.lifecycle.lifecycleScope
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Dialog
import com.google.gson.Gson
import com.stripe.android.PaymentConfiguration
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

private val Ink = Color(0xFF17130F)
private val Cream = Color(0xFFFFF3DF)
private val Orange = Color(0xFFF47716)
private val Gold = Color(0xFFFFB43B)
private val Muted = Color(0xFFB9AA98)
private val Card = Color(0xFF292019)

class MainActivity : ComponentActivity() {
    private lateinit var paymentSheet: PaymentSheet
    private var pendingOrderJson: String = ""
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        paymentSheet = PaymentSheet(this, ::onPaymentSheetResult)
        setContent {
    var showIntro by remember { mutableStateOf(true) }

    if (showIntro) {
        ChonkiesIntro(onFinished = { showIntro = false })
    } else {
        ChonkiesApp(onPay = { cart, customer ->
            startPayment(cart, customer)
        })
    }
}
    }

    private fun startPayment(cart: List<CartLine>, customer: Customer) {
        val payload = mapOf("items" to cart.map { mapOf("id" to it.product.id, "quantity" to it.quantity, "options" to it.options) }, "customer" to customer)
        pendingOrderJson = Gson().toJson(payload)
        lifecycleScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    val client = OkHttpClient.Builder().callTimeout(30, TimeUnit.SECONDS).build()
                    val body = pendingOrderJson.toRequestBody("application/json".toMediaType())
                    val request = Request.Builder().url("${BuildConfig.API_BASE_URL}/create-payment-intent").post(body).build()
                    client.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) error("Payment server returned ${response.code}")
                        Gson().fromJson(response.body!!.string(), PaymentIntentResponse::class.java)
                    }
                }
                PaymentConfiguration.init(applicationContext, result.publishableKey)
                paymentSheet.presentWithPaymentIntent(result.clientSecret, PaymentSheet.Configuration("Chonkies"))
            } catch (e: Exception) { paymentMessage = "Could not start checkout: ${e.message}"; paymentMessageVersion++ }
        }
    }

    private fun onPaymentSheetResult(result: PaymentSheetResult) {
        when (result) {
            is PaymentSheetResult.Completed -> { paymentMessage = "Payment complete! Thanks for ordering Chonkies."; paymentMessageVersion++ }
            is PaymentSheetResult.Canceled -> { paymentMessage = "Payment canceled. Your cart is still here."; paymentMessageVersion++ }
            is PaymentSheetResult.Failed -> { paymentMessage = "Payment failed: ${result.error.localizedMessage}"; paymentMessageVersion++ }
        }
    }
    companion object { var paymentMessage by mutableStateOf(""); var paymentMessageVersion by mutableIntStateOf(0) }
}

private data class Product(val id: String, val name: String, val category: String, val description: String, val priceCents: Int, val emoji: String)
private data class CartLine(val product: Product, val quantity: Int, val options: String = "")
private data class Customer(val name: String, val email: String, val phone: String, val pickup: String)
private data class PaymentIntentResponse(val clientSecret: String, val publishableKey: String)

private val categories = listOf("All", "Burritos", "Tacos", "Quesadillas", "Burrito Bowls", "Shakes", "Smoothies", "Tots", "Fries", "Bubble Tea")
// DEMO PRICES ONLY. Replace these in app and backend catalog before taking real orders.
private val products = listOf(
    Product("burrito-classic", "Build Your Burrito", "Burritos", "Make it yours with your favorite fillings.", 1099, "🌯"),
    Product("burrito-bowl", "Build Your Bowl", "Burrito Bowls", "All the burrito goodness, no tortilla.", 1099, "🥣"),
    Product("taco-trio", "Taco Trio", "Tacos", "Three tacos, loaded your way.", 999, "🌮"),
    Product("quesadilla", "Cheesy Quesadilla", "Quesadillas", "Golden, melty and made to order.", 899, "🫓"),
    Product("shake", "Chonkies Shake", "Shakes", "A thick, creamy treat.", 599, "🥤"),
    Product("smoothie", "Fruit Smoothie", "Smoothies", "A cool, fruity refresh.", 549, "🥭"),
    Product("tots", "Crispy Tots", "Tots", "Golden bites made for dipping.", 399, "🥔"),
    Product("fries", "Seasoned Fries", "Fries", "Hot, crispy and craveable.", 399, "🍟"),
    Product("bubble-tea", "Bubble Tea", "Bubble Tea", "A refreshing favorite with boba.", 599, "🧋")
)

@Composable
private fun ChonkiesIntro(onFinished: () -> Unit) {
    var visibleLetters by remember { mutableIntStateOf(0) }
    val letters = "CHONKIES"

    LaunchedEffect(Unit) {
        for (i in 1..letters.length) {
            delay(120)
            visibleLetters = i
        }
        delay(1300)
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Ink),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(horizontalArrangement = Arrangement.Center) {
                letters.forEachIndexed { index, letter ->
                    val scale by animateFloatAsState(
                        targetValue = if (index < visibleLetters) 1f else 0f,
                        animationSpec = spring(
                            dampingRatio = 0.45f,
                            stiffness = 300f
                        ),
                        label = "letter"
                    )
                    Text(
                        text = letter.toString(),
                        color = if (index % 2 == 0) Orange else Gold,
                        fontSize = 34.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            translationY = (1f - scale) * 45f
                        }
                    )
                }
            }
            Spacer(modifier = Modifier.height(18.dp))
            Text(
                "GETTING THE GOOD STUFF READY...",
                color = Cream,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
        }
    }
}
@Composable
private fun ChonkiesApp(onPay: (List<CartLine>, Customer) -> Unit) {
    var selectedCategory by remember { mutableStateOf("All") }
    var cart by remember { mutableStateOf(listOf<CartLine>()) }
    var selectedProduct by remember { mutableStateOf<Product?>(null) }
    var showCart by remember { mutableStateOf(false) }
    var showCheckout by remember { mutableStateOf(false) }
    var burritoOptions by remember { mutableStateOf("Protein: Chicken • Rice: Cilantro lime • Beans: Black • Extras: Cheese, pico, lettuce • Salsa: Mild") }
    var customerName by remember { mutableStateOf("") }
    var customerEmail by remember { mutableStateOf("") }
    var customerPhone by remember { mutableStateOf("") }
    var notice by remember { mutableStateOf("") }
    val total = cart.sumOf { it.product.priceCents * it.quantity }
    val visible = if (selectedCategory == "All") products else products.filter { it.category == selectedCategory }

    MaterialTheme(colorScheme = darkColorScheme(primary = Orange, background = Ink, surface = Card, onPrimary = Ink, onBackground = Cream, onSurface = Cream)) {
        Scaffold(containerColor = Ink, bottomBar = {
            NavigationBar(containerColor = Color(0xFF211A14), contentColor = Cream) {
                NavigationBarItem(selected = !showCart, onClick = { showCart = false; showCheckout = false }, icon = { Text("⌂", fontSize = 22.sp) }, label = { Text("Order") })
                NavigationBarItem(selected = showCart, onClick = { showCart = true; showCheckout = false }, icon = { Text("🛒", fontSize = 18.sp) }, label = { Text("Cart (${cart.sumOf { it.quantity }})") })
            }
        }) { padding ->
            Column(Modifier.fillMaxSize().padding(padding)) {
                Row(Modifier.fillMaxWidth().background(Ink).padding(horizontal = 20.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("CHONKIES", color = Cream, fontSize = 26.sp, fontWeight = FontWeight.Black, letterSpacing = (-1).sp)
                        Text("GOOD FOOD. BIG FLAVOR.", color = Orange, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                    }
                    Image(painter = painterResource(R.drawable.chonkies_mascot), contentDescription = "Chonkies sunglasses burrito mascot", modifier = Modifier.size(76.dp), contentScale = ContentScale.Fit)
                }
                if (!showCart) {
                    Column(Modifier.fillMaxWidth().background(Color(0xFF2A1B10)).padding(20.dp)) {
                        Text("BIG BITES. BIG VIBES.", color = Gold, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                        Text("What's your craving?", color = Cream, fontSize = 28.sp, fontWeight = FontWeight.Black)
                        Text("Fresh flavor, made your way.", color = Muted, fontSize = 14.sp)
                    }
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 12.dp, horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        categories.forEach { category -> FilterChip(selected = selectedCategory == category, onClick = { selectedCategory = category }, label = { Text(category) }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Orange, selectedLabelColor = Ink, labelColor = Cream)) }
                    }
                    LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(visible) { product -> ProductCard(product) { selectedProduct = product } }
                    }
                } else {
                    if (showCheckout) {
                        CheckoutScreen(customerName, { customerName = it }, customerEmail, { customerEmail = it }, customerPhone, { customerPhone = it }, total, onBack = { showCheckout = false }, onPay = {
                            if (customerName.isBlank() || customerEmail.isBlank()) notice = "Please enter your name and email."
                            else { onPay(cart, Customer(customerName, customerEmail, customerPhone, "Pickup")); notice = "Opening secure payment…" }
                        }, notice = notice)
                    } else {
                        CartScreen(cart, total, onQty = { line, qty -> cart = if (qty <= 0) cart.filterNot { it.product.id == line.product.id && it.options == line.options } else cart.map { if (it == line) it.copy(quantity = qty) else it } }, onCheckout = { if (cart.isNotEmpty()) showCheckout = true })
                    }
                }
                val msg = MainActivity.paymentMessage
                if (msg.isNotBlank()) Text(msg, color = Gold, modifier = Modifier.fillMaxWidth().background(Card).clickable { MainActivity.paymentMessage = "" }.padding(12.dp), textAlign = TextAlign.Center)
            }
        }
        selectedProduct?.let { product ->
            Dialog(onDismissRequest = { selectedProduct = null }) {
                var protein by remember { mutableStateOf("Chicken") }
                var rice by remember { mutableStateOf("Cilantro lime") }
                var beans by remember { mutableStateOf("Black beans") }
                var salsa by remember { mutableStateOf("Mild") }
                Column(Modifier.fillMaxWidth().background(Cream, RoundedCornerShape(24.dp)).padding(20.dp)) {
                    Text(product.name, color = Ink, fontSize = 23.sp, fontWeight = FontWeight.Black)
                    Text(product.description, color = Color(0xFF66594B), fontSize = 13.sp)
                    if (product.category == "Burritos" || product.category == "Burrito Bowls") {
                        Text("MAKE IT YOURS", color = Orange, fontWeight = FontWeight.Black, modifier = Modifier.padding(top = 14.dp))
                        Text("Protein", color = Ink, fontWeight = FontWeight.Bold)
                        OptionRow(listOf("Chicken", "Steak", "Carnitas", "Veggie"), protein) { protein = it }
                        Text("Rice", color = Ink, fontWeight = FontWeight.Bold)
                        OptionRow(listOf("Cilantro lime", "White rice", "No rice"), rice) { rice = it }
                        Text("Beans", color = Ink, fontWeight = FontWeight.Bold)
                        OptionRow(listOf("Black beans", "Pinto beans", "No beans"), beans) { beans = it }
                        Text("Salsa", color = Ink, fontWeight = FontWeight.Bold)
                        OptionRow(listOf("Mild", "Medium", "Hot", "No salsa"), salsa) { salsa = it }
                    }
                    Button(onClick = {
                        val options = if (product.category == "Burritos" || product.category == "Burrito Bowls") "Protein: $protein • Rice: $rice • Beans: $beans • Salsa: $salsa" else "Standard"
                        val existing = cart.find { it.product.id == product.id && it.options == options }
                        cart = if (existing == null) cart + CartLine(product, 1, options) else cart.map { if (it == existing) it.copy(quantity = it.quantity + 1) else it }
                        selectedProduct = null
                    }, modifier = Modifier.fillMaxWidth().padding(top = 16.dp), colors = ButtonDefaults.buttonColors(containerColor = Orange, contentColor = Ink)) { Text("ADD TO ORDER • ${money(product.priceCents)}", fontWeight = FontWeight.Black) }
                }
            }
        }
    }
}

@Composable private fun ProductCard(p: Product, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().background(Card, RoundedCornerShape(18.dp)).clickable(onClick = onClick).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(68.dp).background(Color(0xFF3B2B1E), RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) { Text(p.emoji, fontSize = 35.sp) }
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(p.name, color = Cream, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(p.description, color = Muted, fontSize = 12.sp, lineHeight = 16.sp)
            Text(money(p.priceCents), color = Gold, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
        }
        Text("+", color = Ink, fontWeight = FontWeight.Black, fontSize = 24.sp, modifier = Modifier.background(Orange, CircleShape).padding(horizontal = 11.dp, vertical = 3.dp))
    }
}

@Composable private fun OptionRow(options: List<String>, selected: String, onSelect: (String) -> Unit) {
    Row(Modifier.horizontalScroll(rememberScrollState()).padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        options.forEach { option -> FilterChip(selected = selected == option, onClick = { onSelect(option) }, label = { Text(option, fontSize = 11.sp) }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Orange, selectedLabelColor = Ink, labelColor = Ink)) }
    }
}

@Composable private fun CartScreen(cart: List<CartLine>, total: Int, onQty: (CartLine, Int) -> Unit, onCheckout: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(18.dp)) {
        Text("YOUR ORDER", color = Cream, fontSize = 26.sp, fontWeight = FontWeight.Black)
        Text("Big flavor is just a few taps away.", color = Muted)
        if (cart.isEmpty()) Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { Text("Your cart is looking hungry. Add something delicious!", color = Muted, textAlign = TextAlign.Center) }
        else LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(cart) { line ->
                Column(Modifier.fillMaxWidth().background(Card, RoundedCornerShape(16.dp)).padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) { Text(line.product.name, color = Cream, fontWeight = FontWeight.Bold); Text(line.options, color = Muted, fontSize = 11.sp) }
                        Text(money(line.product.priceCents * line.quantity), color = Gold, fontWeight = FontWeight.Bold)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = { onQty(line, line.quantity - 1) }) { Text("−", color = Orange, fontSize = 22.sp) }
                        Text("${line.quantity}", color = Cream, fontWeight = FontWeight.Bold)
                        TextButton(onClick = { onQty(line, line.quantity + 1) }) { Text("+", color = Orange, fontSize = 22.sp) }
                    }
                }
            }
        }
        if (cart.isNotEmpty()) {
            HorizontalDivider(color = Muted.copy(alpha = .3f))
            Row(Modifier.fillMaxWidth().padding(vertical = 14.dp), horizontalArrangement = Arrangement.SpaceBetween) { Text("Subtotal", color = Cream); Text(money(total), color = Gold, fontWeight = FontWeight.Bold) }
            Text("Tax and any applicable fees are calculated at checkout.", color = Muted, fontSize = 11.sp)
            Button(onClick = onCheckout, modifier = Modifier.fillMaxWidth().padding(top = 10.dp), colors = ButtonDefaults.buttonColors(containerColor = Orange, contentColor = Ink)) { Text("CONTINUE TO CHECKOUT", fontWeight = FontWeight.Black) }
        }
    }
}

@Composable private fun CheckoutScreen(name: String, setName: (String) -> Unit, email: String, setEmail: (String) -> Unit, phone: String, setPhone: (String) -> Unit, total: Int, onBack: () -> Unit, onPay: () -> Unit, notice: String) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("CHECKOUT", color = Cream, fontSize = 26.sp, fontWeight = FontWeight.Black)
        Text("Pickup order", color = Gold, fontWeight = FontWeight.Bold)
        Text("Your details", color = Cream, fontWeight = FontWeight.Bold)
        OutlinedTextField(name, setName, label = { Text("Full name") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        OutlinedTextField(email, setEmail, label = { Text("Email address") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        OutlinedTextField(phone, setPhone, label = { Text("Phone (optional)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        Text("Payment", color = Cream, fontWeight = FontWeight.Bold)
        Text("Secure card and wallet payment powered by Stripe.", color = Muted, fontSize = 13.sp)
        Row(Modifier.fillMaxWidth().background(Card, RoundedCornerShape(14.dp)).padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) { Text("Order subtotal", color = Cream); Text(money(total), color = Gold, fontWeight = FontWeight.Bold) }
        Text("Tax is calculated by the payment server after your menu and tax settings are configured.", color = Muted, fontSize = 11.sp)
        if (notice.isNotBlank()) Text(notice, color = Gold)
        Button(onClick = onPay, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Orange, contentColor = Ink)) { Text("PAY ${money(total)}", fontWeight = FontWeight.Black) }
        TextButton(onClick = onBack) { Text("Back to cart", color = Cream) }
    }
}

private fun money(cents: Int) = "$" + "%.2f".format(cents / 100.0)
