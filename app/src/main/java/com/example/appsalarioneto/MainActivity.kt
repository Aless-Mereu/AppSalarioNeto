// Define el paquete al que pertenece este archivo. Es la forma en que Android organiza el código.
package com.example.appsalarioneto

// Importaciones de todas las librerías y componentes que se usan en el archivo.
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.composable
import com.example.appsalarioneto.ui.theme.AppSalarioNetoTheme

/**
 * Define un conjunto de valores constantes y seguros para el estado civil.
 * Usar un 'enum' evita errores de escritura (p.ej., "soltero" vs "Soltero").
 * @param displayName El texto que se mostrará en la interfaz de usuario.
 */
enum class EstadoCivil(val displayName: String) {
    SOLTERO("Soltero/a"),
    CASADO("Casado/a"),
    DIVORCIADO("Divorciado/a"),
    VIUDO("Viudo/a")
}

/**
 * Define los tipos de pagas anuales permitidas.
 * @param valor El número real que se usará para los cálculos.
 * @param displayName El texto que se mostrará en el menú desplegable.
 */
enum class NumeroPagas(val valor: Int, val displayName: String) {
    DOCE(12, "12 Pagas"),
    CATORCE(14, "14 Pagas")
}

/**
 * La actividad principal de la aplicación, el punto de entrada.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Habilita que la app se dibuje de borde a borde de la pantalla.
        enableEdgeToEdge()
        // Define el contenido de la actividad usando Jetpack Compose.
        setContent {
            // Aplica el tema de Material Design definido en AppSalarioNetoTheme.
            AppSalarioNetoTheme {
                // Llama al Composable principal que contiene toda la lógica de la UI.
                MyApp()
            }
        }
    }
}

/**
 * Función de lógica pura que calcula el salario neto.
 * No depende de la interfaz, solo recibe datos y devuelve un resultado.
 *
 * @param salarioBruto El salario anual antes de impuestos.
 * @param numPagas El número de pagas (12 o 14).
 * @param edad La edad del empleado.
 * @param numHijos El número de hijos.
 * @param estadoCivil El estado civil del empleado.
 * @param discapacidad El grado de discapacidad.
 * @return El salario neto por cada paga (salario neto anual / numPagas).
 */
fun calcularSalarioNeto(
    salarioBruto: Int,
    numPagas: Int,
    edad: Int,
    numHijos: Int,
    estadoCivil: String,
    discapacidad: Int
): Double {
    // NOTA: Este es un cálculo de IRPF muy simplificado solo para el ejemplo.

    // 1. Determina un tipo de retención base según el salario bruto.
    val irpfBase = when {
        salarioBruto < 20000 -> 15
        salarioBruto < 40000 -> 20
        else -> 25
    }

    // 2. Aplica pequeños ajustes (descuentos) a ese tipo de retención.
    val descuentoPorHijos = numHijos * 1  // Ejemplo: 1% de descuento por cada hijo.
    val descuentoPorDiscapacidad = if (discapacidad > 0) 2 else 0 // Ejemplo: 2% si hay discapacidad.

    // 3. Calcula el porcentaje final de IRPF a aplicar.
    val irpfTotal = irpfBase - descuentoPorHijos - descuentoPorDiscapacidad

    // 4. Calcula el salario neto anual restando el IRPF al bruto.
    val salarioNeto = salarioBruto * (1 - irpfTotal / 100.0)

    // 5. Devuelve el importe neto que corresponde a cada paga.
    return salarioNeto / numPagas
}

/**
 * El Composable principal que gestiona el estado de la aplicación.
 * Decide si mostrar la pantalla de formulario (InputScreen) o la de resultados (ResultScreen).
 * Es el "cerebro" de la interfaz.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
fun MyApp() {
    // --- GESTIÓN DEL ESTADO ---
    // 'rememberSaveable' guarda el estado incluso si la app se destruye (p.ej. al girar la pantalla).
    var salarioBruto by rememberSaveable { mutableStateOf("") }
    var numPagas by rememberSaveable { mutableStateOf(NumeroPagas.DOCE) }
    var edad by rememberSaveable { mutableStateOf("") }
    var numHijos by rememberSaveable { mutableStateOf("") }
    var estadoCivil by rememberSaveable { mutableStateOf(EstadoCivil.SOLTERO) }
    var discapacidad by rememberSaveable { mutableStateOf("") }

    // Estado para controlar la navegación entre pantallas.
    var mostrarResultado by rememberSaveable { mutableStateOf(false) }
    var mensajeResultado by rememberSaveable { mutableStateOf("") }

    // --- LÓGICA DE NAVEGACIÓN ---
    if (mostrarResultado) {
        // Si 'mostrarResultado' es true, muestra la pantalla de resultados.
        ResultScreen(
            mensaje = mensajeResultado,
            onVolver = { mostrarResultado = false } // Lambda para volver al formulario.
        )
    } else {
        // Si no, muestra la pantalla del formulario.
        InputScreen(
            // Pasa los estados actuales al Composable de la UI.
            salarioBruto = salarioBruto,
            numPagas = numPagas,
            edad = edad,
            numHijos = numHijos,
            estadoCivil = estadoCivil,
            discapacidad = discapacidad,

            // Pasa las funciones 'lambda' que permitirán a InputScreen modificar el estado.
            onSalarioChange = { salarioBruto = it },
            onPagasChange = { numPagas = it },
            onEdadChange = { edad = it },
            onHijosChange = { numHijos = it },
            onEstadoCivilChange = { estadoCivil = it },
            onDiscapacidadChange = { discapacidad = it },

            // Lambda que se ejecuta cuando se pulsa el botón "Calcular".
            onCalcular = {
                // Convierte los textos de la UI a números. 'toIntOrNull' devuelve null si no es un número válido.
                val salario = salarioBruto.toIntOrNull()
                val pagas = numPagas.valor // Obtiene el valor Int del enum.
                val edadInt = edad.toIntOrNull() ?: 30 // Si es nulo, usa 30 por defecto.
                val hijos = numHijos.toIntOrNull() ?: 0 // Si es nulo, usa 0.
                val disc = discapacidad.toIntOrNull() ?: 0

                if (salario != null) {
                    // Si el salario es válido, llama a la función de cálculo.
                    val neto =
                        calcularSalarioNeto(
                            salario,
                            pagas,
                            edadInt,
                            hijos,
                            estadoCivil.displayName, // Pasa el String del enum.
                            disc
                        )
                    // Prepara el mensaje y cambia el estado para mostrar la pantalla de resultados.
                    mensajeResultado = "Salario neto por paga mensual: ${"%.2f".format(neto)}€"
                    mostrarResultado = true
                } else {
                    // Si el salario no es válido, muestra un mensaje de error.
                    mensajeResultado = "Introduce un salario válido"
                    mostrarResultado = true
                }
            }
        )
    }
}



/**
 * Composable que define la pantalla del formulario de entrada de datos.
 * Es una "UI tonta" (dumb component): solo muestra datos y notifica eventos, no tiene lógica propia.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InputScreen(
    salarioBruto: String,
    onSalarioChange: (String) -> Unit,
    numPagas: NumeroPagas,
    onPagasChange: (NumeroPagas) -> Unit,
    edad: String,
    onEdadChange: (String) -> Unit,
    numHijos: String,
    onHijosChange: (String) -> Unit,
    estadoCivil: EstadoCivil,
    onEstadoCivilChange: (EstadoCivil) -> Unit,
    discapacidad: String,
    onDiscapacidadChange: (String) -> Unit,
    onCalcular: () -> Unit
) {
    // Scaffold proporciona la estructura básica de Material Design (barra superior, contenido, etc.).
    Scaffold(
        topBar = {
            TopAppBar(
                colors = topAppBarColors(
                    // Personaliza los colores de la barra.
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                ),
                title = {
                    Text(
                        "Calculadora de Salario Neto",
                        textAlign = TextAlign.Center, // Centra el texto horizontalmente.
                        modifier = Modifier.fillMaxWidth() // Hace que el Text ocupe todo el ancho disponible.
                    )
                }
            )
        }
    ) { innerPadding -> // 'innerPadding' es el espacio que deja la TopAppBar para que el contenido no se solape.
        Surface(
            modifier = Modifier
                .padding(innerPadding) // Aplica el padding para no quedar debajo de la TopAppBar.
                .fillMaxSize(), // Ocupa toda la pantalla.
            shape = MaterialTheme.shapes.large,
            color = Color(0xFFB2A9BB), // Color de fondo personalizado.
            shadowElevation = 10.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally // Centra todos los elementos del Column.
            ) {
                // Estado local para controlar si el menú desplegable de pagas está abierto.
                var pagasExpanded by remember { mutableStateOf(false) }

                // Campo de texto para el salario.
                TextField(
                    salarioBruto,
                    onValueChange = onSalarioChange,
                    label = { Text("Salario bruto") })

                Spacer(modifier = Modifier.height(24.dp)) // Espacio vertical.

                // Componente para menús desplegables.
                ExposedDropdownMenuBox(
                    expanded = pagasExpanded,
                    onExpandedChange = { pagasExpanded = !pagasExpanded }
                ) {
                    // Este es el campo de texto falso que muestra la selección actual.
                    TextField(
                        value = numPagas.displayName,
                        onValueChange = {}, // No hace nada al escribir.
                        readOnly = true, // No se puede editar manualmente.
                        label = { Text("Número de pagas") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = pagasExpanded) },
                        modifier = Modifier.menuAnchor() // Vincula este TextField al menú.
                    )
                    // Este es el menú que aparece.
                    ExposedDropdownMenu(
                        expanded = pagasExpanded,
                        onDismissRequest = { pagasExpanded = false } // Se cierra si se pulsa fuera.
                    ) {
                        // Itera sobre todos los valores del enum 'NumeroPagas'.
                        NumeroPagas.values().forEach { paga ->
                            DropdownMenuItem(
                                text = { Text(paga.displayName) },
                                onClick = {
                                    onPagasChange(paga) // Notifica el cambio.
                                    pagasExpanded = false // Cierra el menú.
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                TextField(
                    edad,
                    onValueChange = onEdadChange,
                    label = { Text("Edad") })

                Spacer(modifier = Modifier.height(24.dp))

                TextField(
                    numHijos,
                    onValueChange = onHijosChange,
                    label = { Text("Número de hijos") })

                Spacer(modifier = Modifier.height(24.dp))

                // Menú desplegable para Estado Civil (misma lógica que el de pagas).
                var estadoCivilExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = estadoCivilExpanded,
                    onExpandedChange = { estadoCivilExpanded = !estadoCivilExpanded }
                ) {
                    TextField(
                        value = estadoCivil.displayName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Estado civil") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = estadoCivilExpanded) },
                        modifier = Modifier.menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = estadoCivilExpanded,
                        onDismissRequest = { estadoCivilExpanded = false }
                    ) {
                        EstadoCivil.values().forEach { estado ->
                            DropdownMenuItem(
                                text = { Text(estado.displayName) },
                                onClick = {
                                    onEstadoCivilChange(estado)
                                    estadoCivilExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                TextField(
                    discapacidad,
                    onValueChange = onDiscapacidadChange,
                    label = { Text("Grado de discapacidad") })

                Spacer(modifier = Modifier.height(24.dp))

                // Botón que, al ser pulsado, ejecuta la lambda 'onCalcular'.
                Button(onClick = onCalcular) {
                    Text("Calcular")
                }
            }
        }
    }
}

/**
 * Composable que define la pantalla de resultados.
 * Es otro "dumb component" que solo muestra el mensaje y un botón para volver.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultScreen(mensaje: String, onVolver: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar( // La misma barra superior que en la pantalla de entrada.
                colors = topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                ),
                title = {
                    Text(
                        "Calculadora de Salario Neto",
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            )
        }
    ) { innerPadding ->
        Surface( // El mismo fondo que en la pantalla de entrada.
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            shape = MaterialTheme.shapes.large,
            color = Color(0xFFB2A9BB),
            shadowElevation = 10.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize() // fillMaxSize para que el centrado vertical funcione.
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally, // Centra horizontalmente.
                verticalArrangement = Arrangement.Center // Centra verticalmente.
            ) {
                // Muestra el mensaje con el resultado.
                Text(text = mensaje, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                // Botón que ejecuta la lambda 'onVolver' para regresar al formulario.
                Button(onClick = onVolver) {
                    Text("Volver")
                }
            }
        }
    }
}
