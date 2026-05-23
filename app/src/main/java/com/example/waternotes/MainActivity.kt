package com.example.waternotes

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Tela Home (MainActivity)
 * Mostra o progresso diário de água do usuário, ações rápidas de inserção e resumo semanal.
 */
class MainActivity : AppCompatActivity() {

    // Views do Layout
    private lateinit var tvGreeting: TextView
    private lateinit var tvPorcentagem: TextView
    private lateinit var progressBarAgua: ProgressBar
    private lateinit var tvProgressoTexto: TextView
    private lateinit var btnAgua250: LinearLayout
    private lateinit var btnAgua500: LinearLayout
    private lateinit var tvResumoSemanal: TextView
    private lateinit var btnSair: ImageButton

    // Views do Gráfico Semanal
    private lateinit var tvValDay1: TextView
    private lateinit var tvValDay2: TextView
    private lateinit var tvValDay3: TextView
    private lateinit var tvValDay4: TextView
    private lateinit var tvValDay5: TextView
    private lateinit var tvValDay6: TextView
    private lateinit var tvValDay7: TextView

    private lateinit var viewFillDay1: View
    private lateinit var viewFillDay2: View
    private lateinit var viewFillDay3: View
    private lateinit var viewFillDay4: View
    private lateinit var viewFillDay5: View
    private lateinit var viewFillDay6: View
    private lateinit var viewFillDay7: View

    private lateinit var tvLabelDay1: TextView
    private lateinit var tvLabelDay2: TextView
    private lateinit var tvLabelDay3: TextView
    private lateinit var tvLabelDay4: TextView
    private lateinit var tvLabelDay5: TextView
    private lateinit var tvLabelDay6: TextView
    private lateinit var tvLabelDay7: TextView

    // Banco de dados e Sessão
    private lateinit var dbHelper: DatabaseHelper
    private var usuarioId: Int = -1
    private var metaDiariaMl: Int = 2000 // Valor padrão de fallback

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Inicializa o banco de dados
        dbHelper = DatabaseHelper(this)

        // Recupera o ID do usuário da Intent ou das SharedPreferences
        recuperarSessaoUsuario()

        // Se o usuário não for válido, retorna para a tela de login por segurança
        if (usuarioId == -1) {
            irParaLogin()
            return
        }

        // Vincula os componentes XML
        inicializarViews()

        // Carrega as informações iniciais e saúda o usuário
        carregarDadosUsuario()

        // Atualiza a tela com o consumo atual de hoje e da semana
        atualizarMetricasProgresso()

        // Configura os ouvintes de clique nos botões
        configurarCliques()
    }

    /**
     * Tenta obter o ID do usuário logado via Intent ou SharedPreferences de backup.
     */
    private fun recuperarSessaoUsuario() {
        // Tenta pegar da Intent de transição direta
        usuarioId = intent.getIntExtra("USUARIO_ID", -1)

        // Caso não encontre (ex: reinício do app), recupera das SharedPreferences persistentes
        if (usuarioId == -1) {
            val sharedPrefs = getSharedPreferences("WaternotesPrefs", Context.MODE_PRIVATE)
            usuarioId = sharedPrefs.getInt("USUARIO_ID", -1)
        }
    }

    /**
     * Localiza as views declaradas no XML e as associa com os atributos.
     */
    private fun inicializarViews() {
        tvGreeting = findViewById(R.id.tvGreeting)
        tvPorcentagem = findViewById(R.id.tvPorcentagem)
        progressBarAgua = findViewById(R.id.progressBarAgua)
        tvProgressoTexto = findViewById(R.id.tvProgressoTexto)
        btnAgua250 = findViewById(R.id.btnAgua250)
        btnAgua500 = findViewById(R.id.btnAgua500)
        tvResumoSemanal = findViewById(R.id.tvResumoSemanal)
        btnSair = findViewById(R.id.btnSair)

        // Inicializa as views do Gráfico Semanal
        tvValDay1 = findViewById(R.id.tvValDay1)
        tvValDay2 = findViewById(R.id.tvValDay2)
        tvValDay3 = findViewById(R.id.tvValDay3)
        tvValDay4 = findViewById(R.id.tvValDay4)
        tvValDay5 = findViewById(R.id.tvValDay5)
        tvValDay6 = findViewById(R.id.tvValDay6)
        tvValDay7 = findViewById(R.id.tvValDay7)

        viewFillDay1 = findViewById(R.id.viewFillDay1)
        viewFillDay2 = findViewById(R.id.viewFillDay2)
        viewFillDay3 = findViewById(R.id.viewFillDay3)
        viewFillDay4 = findViewById(R.id.viewFillDay4)
        viewFillDay5 = findViewById(R.id.viewFillDay5)
        viewFillDay6 = findViewById(R.id.viewFillDay6)
        viewFillDay7 = findViewById(R.id.viewFillDay7)

        tvLabelDay1 = findViewById(R.id.tvLabelDay1)
        tvLabelDay2 = findViewById(R.id.tvLabelDay2)
        tvLabelDay3 = findViewById(R.id.tvLabelDay3)
        tvLabelDay4 = findViewById(R.id.tvLabelDay4)
        tvLabelDay5 = findViewById(R.id.tvLabelDay5)
        tvLabelDay6 = findViewById(R.id.tvLabelDay6)
        tvLabelDay7 = findViewById(R.id.tvLabelDay7)
    }

    /**
     * Busca os dados cadastrais do usuário para calcular a meta diária e exibir a saudação.
     */
    private fun carregarDadosUsuario() {
        val usuario = dbHelper.buscarUsuarioPorId(usuarioId)
        if (usuario != null) {
            // Define o nome de saudação na tela
            tvGreeting.text = "Olá, ${usuario.nome}!"

            // Calcula a meta com base na regra de peso: Peso * 35 ml
            metaDiariaMl = (usuario.peso * 35).toInt()
        } else {
            // Em caso de falha rara ao buscar o usuário
            tvGreeting.text = "Olá!"
        }
    }

    /**
     * Consulta o SQLite para ler a soma de hoje e da semana e atualiza as views na tela de forma reativa.
     */
    private fun atualizarMetricasProgresso() {
        // Obtém a data de hoje no formato YYYY-MM-DD
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val hoje = format.format(Date())

        // Consulta a soma de água consumida hoje
        val totalHojeMl = dbHelper.buscarAguaDoDia(usuarioId, hoje)

        // Consulta a soma de água consumida na semana (últimos 7 dias)
        val totalSemanaMl = dbHelper.buscarAguaDaSemana(usuarioId)

        // --- ATUALIZAÇÕES DIÁRIAS ---
        
        // Texto descritivo do progresso
        tvProgressoTexto.text = "Você bebeu ${totalHojeMl}ml de ${metaDiariaMl}ml hoje"

        // Progresso percentual da barra
        val porcentagem = if (metaDiariaMl > 0) {
            ((totalHojeMl.toDouble() / metaDiariaMl) * 100).toInt()
        } else {
            0
        }

        // Define a porcentagem no TextView e atualiza a barra de progresso
        tvPorcentagem.text = "$porcentagem%"
        progressBarAgua.max = metaDiariaMl
        progressBarAgua.progress = totalHojeMl

        // --- ATUALIZAÇÕES SEMANAIS ---
        
        // Define o texto na seção do resumo semanal
        tvResumoSemanal.text = "Total bebido nos últimos 7 dias: ${totalSemanaMl} ml"

        // Atualiza o gráfico semanal de forma dinâmica
        atualizarGraficoSemanal()
    }

    /**
     * Atualiza o gráfico semanal de consumo de água de forma dinâmica.
     */
    private fun atualizarGraficoSemanal() {
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val calendar = java.util.Calendar.getInstance()

        // Criamos uma lista de pares contendo a data (String) e o dia da semana formatado (S, T, Q, Q, S, S, D)
        val diasSemana = ArrayList<Pair<String, String>>()

        // Adiciona os dias de trás para frente (do mais antigo ao mais recente/hoje)
        calendar.add(java.util.Calendar.DAY_OF_YEAR, -6)
        for (i in 0 until 7) {
            val dateStr = format.format(calendar.time)
            
            // Mapeia o dia da semana de forma robusta
            val label = when (calendar.get(java.util.Calendar.DAY_OF_WEEK)) {
                java.util.Calendar.SUNDAY -> "D"
                java.util.Calendar.MONDAY -> "S"
                java.util.Calendar.TUESDAY -> "T"
                java.util.Calendar.WEDNESDAY -> "Q"
                java.util.Calendar.THURSDAY -> "Q"
                java.util.Calendar.FRIDAY -> "S"
                java.util.Calendar.SATURDAY -> "S"
                else -> "-"
            }
            
            diasSemana.add(Pair(dateStr, label))
            calendar.add(java.util.Calendar.DAY_OF_YEAR, 1) // Avança para o próximo dia
        }

        // Listas das views para iteração simples
        val tvValores = listOf(tvValDay1, tvValDay2, tvValDay3, tvValDay4, tvValDay5, tvValDay6, tvValDay7)
        val viewFills = listOf(viewFillDay1, viewFillDay2, viewFillDay3, viewFillDay4, viewFillDay5, viewFillDay6, viewFillDay7)
        val tvLabels = listOf(tvLabelDay1, tvLabelDay2, tvLabelDay3, tvLabelDay4, tvLabelDay5, tvLabelDay6, tvLabelDay7)

        val density = resources.displayMetrics.density
        val maxHeightPx = (80 * density).toInt() // Altura do FrameLayout definida no XML (80dp)

        val maxBase = if (metaDiariaMl > 0) metaDiariaMl.toDouble() else 2000.0

        for (idx in 0 until 7) {
            val dia = diasSemana[idx]
            val qtd = dbHelper.buscarAguaDoDia(usuarioId, dia.first)

            // Atualiza os textos
            tvValores[idx].text = qtd.toString()
            tvLabels[idx].text = dia.second

            // Se for o dia de hoje, destaca a cor do texto do dia para facilitar a leitura
            if (idx == 6) {
                tvLabels[idx].setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.blue_primary))
            } else {
                tvLabels[idx].setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.text_dark))
            }

            // Calcula o preenchimento da barra
            val percent = Math.min(1.0, qtd.toDouble() / maxBase)
            val params = viewFills[idx].layoutParams
            params.height = (maxHeightPx * percent).toInt()
            viewFills[idx].layoutParams = params
        }
    }

    /**
     * Configura as ações de clique para inserir consumo ou efetuar logout.
     */
    private fun configurarCliques() {
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val hoje = format.format(Date())

        // Botão rápido: Adicionar 250ml
        btnAgua250.setOnClickListener {
            registrarConsumoAgua(250, hoje)
        }

        // Botão rápido: Adicionar 500ml
        btnAgua500.setOnClickListener {
            registrarConsumoAgua(500, hoje)
        }

        // Botão de Logout (Sair)
        btnSair.setOnClickListener {
            logoutUsuario()
        }
    }

    /**
     * Executa a inserção da água no banco e atualiza a interface instantaneamente.
     */
    private fun registrarConsumoAgua(quantidade: Int, data: String) {
        val inseriu = dbHelper.inserirAgua(usuarioId, quantidade, data)
        if (inseriu) {
            Toast.makeText(this, "Sucesso! +${quantidade}ml registrados.", Toast.LENGTH_SHORT).show()
            // Recarrega os dados imediatamente na tela para feedback em tempo real
            atualizarMetricasProgresso()
        } else {
            Toast.makeText(this, "Erro ao registrar o consumo.", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Limpa o ID das SharedPreferences para fechar a sessão e redireciona ao Login.
     */
    private fun logoutUsuario() {
        val sharedPrefs = getSharedPreferences("WaternotesPrefs", Context.MODE_PRIVATE)
        sharedPrefs.edit().remove("USUARIO_ID").apply()
        
        Toast.makeText(this, "Até logo!", Toast.LENGTH_SHORT).show()
        irParaLogin()
    }

    /**
     * Redireciona o usuário para a LoginActivity e finaliza a MainActivity.
     */
    private fun irParaLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }
}