package com.example.olympus_gym_preview.presentation

import OlympusGymTheme
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.example.olympus_gym_preview.R

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.wear.compose.material.Scaffold
import androidx.compose.material3.TextField
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Constraints
import androidx.core.app.NotificationCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import androidx.wear.compose.material.Vignette
import androidx.wear.compose.material.VignettePosition
import androidx.wear.compose.material.scrollAway
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.olympus_gym_preview.presentation.data.model.MembershipRepository
import com.example.olympus_gym_preview.presentation.local.db.AppDatabase
import com.example.olympus_gym_preview.presentation.local.db.MembershipDomain
import com.example.olympus_gym_preview.presentation.network.ApiClient
import com.example.olympus_gym_preview.presentation.network.VerificationRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class MembershipCheckWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val database = AppDatabase.getDatabase(applicationContext)
        val repository = MembershipRepository(database.membershipDao(), ApiClient)

        val membership = repository.getAllMemberships().first().firstOrNull()

        membership?.let {
            val daysRemaining = calculateDaysRemaining(it.expirationDate)
            if (daysRemaining in 1..5) {
                showNotification(applicationContext, daysRemaining)
            }
        }

        return Result.success()
    }

    private suspend fun showNotification(context: Context, daysRemaining: Int) {
        withContext(Dispatchers.Main) {
            val notificationId = daysRemaining // Usamos los días como ID único

            val notification = NotificationCompat.Builder(context, "membership_channel")
                .setSmallIcon(R.drawable.olympus_gym) // Asegúrate de tener este icono
                .setContentTitle("Tu membresía está por expirar")
                .setContentText("Faltan $daysRemaining días para que expire tu membresía")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setAutoCancel(true)
                .build()

            Log.d("Notification", "Showing expiration notification from Worker")

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(notificationId, notification)
        }
    }
}

class MainActivity : ComponentActivity() {
    //Database
    private lateinit var database: AppDatabase
    private lateinit var repository: MembershipRepository
    private var hasMembership by mutableStateOf(false)

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            "membership_channel",
            "Recordatorios de Membresía",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Notificaciones sobre el estado de tu membresía"
            setShowBadge(true)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }

        Log.d("NotificationChannel", "Creating notification channel")

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }

    private fun showExpirationNotification(daysRemaining: Int) {
        val notificationId = daysRemaining // Usamos los días como ID único

        val notification = NotificationCompat.Builder(this, "membership_channel")
            .setSmallIcon(R.drawable.olympus_gym) // Asegúrate de tener este icono
            .setContentTitle("Tu membresía está por expirar")
            .setContentText("Faltan $daysRemaining días para que expire tu membresía")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .build()

        Log.d("Notification", "Showing expiration notification")

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(notificationId, notification)
    }

    private val verificationScope = CoroutineScope(Dispatchers.Default)

    private fun startExperimentalCheck() {
        Log.d("ExperimentalCheck", "Starting experimental check")
        verificationScope.launch {
            while (true) { // Bucle infinito (solo para pruebas)
                val membership = repository.getAllMemberships().first().firstOrNull()
                membership?.let {
                    val daysRemaining = calculateDaysRemaining(it.expirationDate)
                    if (daysRemaining in 1..7) {
                        withContext(Dispatchers.Main) {
                            showExpirationNotification(daysRemaining)
                        }
                    }
                }
                delay(20_000) // Espera 15 segundos
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        createNotificationChannel()

        // Start databse
        database = AppDatabase.getDatabase(this)
        repository = MembershipRepository(database.membershipDao(), ApiClient)

        super.onCreate(savedInstanceState)

        setTheme(android.R.style.Theme_Black)

        scheduleDailyCheck()
        startExperimentalCheck()

        setContent {
            OlympusGymTheme {
                val navController = rememberSwipeDismissableNavController()

                isMemberActive()

                LaunchedEffect(hasMembership) {
                    if (hasMembership) {
                       repository.refreshMembershipFromBackend()
                        navController.navigate(Screen.Membership.route) {
                            popUpTo(Screen.Welcome.route) { inclusive = true }
                        }
                    }
                }

                SwipeDismissableNavHost(
                    navController = navController,
                    startDestination = Screen.Welcome.route
                ) {
                    composable(Screen.Welcome.route) { WelcomeScreen(navController) }

                    composable(Screen.Verification.route) {
                        VerificationScreen(navController, repository)
                    }

                    composable(Screen.Membership.route) {
                        MembershipScreen(repository){
                            logout(repository, navController)
                        }
                    }
                }
            }
        }
    }

    fun isMemberActive() {
        lifecycleScope.launch {
            val records = withContext(Dispatchers.IO) {
                repository.getAllMemberships().first()
            }

            hasMembership = records.isNotEmpty()

            if (hasMembership) {
                val expirationDate = records.first().expirationDate
                val daysRemaining = calculateDaysRemaining(expirationDate)

                Log.d("ExpirationDate", "Days remaining: $daysRemaining")

                if (daysRemaining in 1..7) {
                    //showExpirationNotification(daysRemaining)
                }
            }
        }
    }

    fun logout(repository: MembershipRepository, navController: NavController) {
        lifecycleScope.launch {
            repository.clearMemberships()
            navController.navigate(Screen.Welcome.route) {
                popUpTo(0)
            }
        }
    }

    private fun scheduleDailyCheck() {
        val constraints = androidx.work.Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val workRequest = PeriodicWorkRequestBuilder<MembershipCheckWorker>(
            24, TimeUnit.HOURS // Verifica cada 24 horas
        ).setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "membership_check",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }
}

//@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true)
@Composable
fun WelcomeScreen(
    navController: NavController
) {
    val scrollState = rememberScalingLazyListState()

    ScalingLazyColumn(
        state = scrollState,
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
    ) {
        // Logo
        item {
            Image(
                painter = painterResource(id = R.drawable.olympus_gym),
                contentDescription = "Logo de Olympus GYM",
                modifier = Modifier.size(55.dp),
                contentScale = ContentScale.Fit
            )
        }

        // Títle
        item {
            Text(
                text = "OLYMPUS GYM",
                style = MaterialTheme.typography.title2,
                fontWeight = FontWeight.ExtraBold
            )
        }

        // Description
        item {
            Text(
                text = "Bienvenido a tu gestión de membresías",
                style = MaterialTheme.typography.caption3,
                textAlign = TextAlign.Center
            )
        }

        // Start button
        item {
            Button(
                onClick = {
                    navController.navigate(Screen.Verification.route)
                },
                modifier = Modifier
                    .fillMaxWidth(.7f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colors.primary,
                    contentColor = MaterialTheme.colors.onPrimary
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text(
                        text = "Comenzar",
                        style = MaterialTheme.typography.caption2,
                    )
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = "Continuar",
                        modifier = Modifier.size(18.dp),
                        tint = Color.White
                    )
                }
            }
        }
    }
}

@SuppressLint("SuspiciousIndentation")
@Composable
fun VerificationScreen(
    navController: NavController,
    repository: MembershipRepository,
) {
    var verificationCode = remember { mutableStateOf("") }
    val context = LocalContext.current

    val coroutineScope = rememberCoroutineScope()

    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
    ) {
        item {
            Image(
                painter = painterResource(R.drawable.olympus_gym),
                contentDescription = "Verificación",
                modifier = Modifier.size(55.dp)
            )
        }

        item {
            Text(
                text = "Ingresa tu código",
                style = MaterialTheme.typography.title2
            )
        }

        item {
            TextField(
                value = verificationCode.value, // Correct: pass the String value
                onValueChange = {
                    if (it.length <= 6) verificationCode.value = it
                }, // Correct: update the String value
                label = {
                    Text(
                        text = "Código de 6 dígitos",
                        style = MaterialTheme.typography.caption2,
                        color = Color.Black
                    )
                },
                shape = MaterialTheme.shapes.medium,
                modifier =
                    Modifier
                        .fillMaxWidth(.6f)
                        .heightIn(min = 48.dp),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done
                )
            )
        }

        item {
            Button(
                onClick = {
                    if (verificationCode.value.length == 6) {
                        Log.d("API Request", "Sending verification code: ${verificationCode.value}")

                        coroutineScope.launch {
                            try {
                                val response = ApiClient.instance.verifyMember(
                                    VerificationRequest(verificationCode.value)
                                )

                                if (response.isSuccessful) {
                                    Log.d("API Response", "Success: ${response.body()}")
                                    response.body()?.let {apiResponse ->
                                        val membership = MembershipDomain(
                                            id = apiResponse.membership.id,
                                            verificationCode = apiResponse.membership.verification_code,
                                            status = apiResponse.membership.status,
                                            expirationDate = parseDate(apiResponse.membership.expiration_date),
                                            firstName = apiResponse.membership.user.firstName,
                                            lastName = apiResponse.membership.user.lastName
                                        )

                                        repository.saveMembership(membership)
                                        navController.navigate(Screen.Membership.route)
                                        Log.d("API Response", "Saving membership: ${membership}")
                                    }
                                } else {
                                    Toast.makeText(
                                        context,
                                        "Código no válido",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            } catch (e: Exception) {
                                Log.e("API Error", e.message ?: "Unknown error")
                                Toast.makeText(
                                    context,
                                    "Error de conexión: ${e.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    } else {
                        Toast.makeText(
                            context,
                            "El código debe tener 6 dígitos",
                            Toast.LENGTH_SHORT,
                        ).show()
                    }
                },
                modifier = Modifier.fillMaxWidth(.7f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colors.primary,
                    contentColor = MaterialTheme.colors.onPrimary
                )
            ) {
                Text("Verificar")
            }
        }
    }
}

/// Membership Screen
@Composable
fun MembershipScreen(repository: MembershipRepository, onLogout: () -> Unit) {
    val scrollState = rememberScalingLazyListState()
    val membership by repository.getAllMemberships().collectAsState(initial = emptyList())

    if (membership.isEmpty()) return

    Log.d("MembershipScreen", "Membership ID: ${membership.first().id}")

    Scaffold(
        timeText = { TimeText(modifier = Modifier.scrollAway(scrollState)) },
        vignette = { Vignette(vignettePosition = VignettePosition.Bottom) }
    ) {
        ScalingLazyColumn(
            state = scrollState,
            modifier =
                Modifier
                    .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
        ) {

            item {
                Text(
                    text = "Estado de tu membresía",
                    style = MaterialTheme.typography.title2,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
            }

            item {
                MembershipStatusCard(
                    userName = "${membership[0].firstName} ${membership[0].lastName}",
                    status = membership[0].status,
                    expirationDate = formatDate(membership[0].expirationDate),
                    daysRemaining = calculateDaysRemaining(membership[0].expirationDate)
                )
            }

            item {
                // Botón de acción
                Button(
                    onClick = onLogout,
                    modifier = Modifier.fillMaxWidth(.8f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colors.primary,
                        contentColor = MaterialTheme.colors.onPrimary
                    )
                ) {
                    Text(
                        text = "Cerrar sesión",
                        style = MaterialTheme.typography.body2
                    )
                }
            }
        }
    }
}



private fun calculateDaysRemaining(expirationDate: Date): Int {
    val diff = expirationDate.time - Date().time
    return TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS).toInt()
}

private fun formatDate(date: Date): String {
    return SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(date)
}

fun parseDate(dateString: String): Date {
    return try {
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
            .parse(dateString) ?: Date()
    } catch (e: Exception) {
        Log.e("DateParse", "Error parsing date: ${e.message}")
        Date()
    }
}


@Composable
fun MembershipStatusCard(
    userName: String,
    status: String,
    expirationDate: String,
    daysRemaining: Int
) {
    Card(
        modifier = Modifier
            .fillMaxWidth(.9f),
        onClick = { /* Acción al hacer clic */ },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colors.secondary, // Fondo
            contentColor = MaterialTheme.colors.onPrimary  // Texto/iconos
        )
    ) {
        Column(
            modifier =
                Modifier
                    .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            InfoRow(label = "Usuario:", value = userName)
            InfoRow(label = "Estado:", value = status, isHighlighted = true)
            InfoRow(label = "Vence:", value = expirationDate)

            // Indicador de días restantes
            LinearProgressIndicator(
                progress = daysRemaining / 30f,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = when {
                    daysRemaining <= 3 -> Color.Red
                    daysRemaining <= 7 -> Color.Yellow
                    else -> Color.Green
                }
            )

            Text(
                text = "$daysRemaining días restantes",
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
fun InfoRow(label: String, value: String, isHighlighted: Boolean = false) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
    ) {
        Text(
            text = label,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f),
            modifier = Modifier.padding(bottom = 2.dp)
        )
        Text(
            text = value,
            fontWeight = if (isHighlighted) FontWeight.Bold else FontWeight.Normal,
            color = if (isHighlighted) MaterialTheme.colors.error else MaterialTheme.colors.onSurface
        )
    }
}