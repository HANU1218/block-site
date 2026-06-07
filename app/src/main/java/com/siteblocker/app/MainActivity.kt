package com.siteblocker.app

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var editTextKeyword: EditText
    private lateinit var btnAdd: Button
    private lateinit var btnEnableService: Button
    private lateinit var statusCard: CardView
    private lateinit var tvStatus: TextView
    private lateinit var tvEmpty: TextView
    private lateinit var adapter: KeywordAdapter
    private val keywords = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        recyclerView     = findViewById(R.id.recyclerView)
        editTextKeyword  = findViewById(R.id.editTextKeyword)
        btnAdd           = findViewById(R.id.btnAdd)
        btnEnableService = findViewById(R.id.btnEnableService)
        statusCard       = findViewById(R.id.statusCard)
        tvStatus         = findViewById(R.id.tvStatus)
        tvEmpty          = findViewById(R.id.tvEmpty)

        adapter = KeywordAdapter(keywords) { keyword ->
            KeywordManager.removeKeyword(this, keyword)
            loadKeywords()
            Toast.makeText(this, "\"$keyword\" removed", Toast.LENGTH_SHORT).show()
        }

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        btnAdd.setOnClickListener {
            val keyword = editTextKeyword.text.toString()
            when {
                keyword.isBlank() ->
                    Toast.makeText(this, "Please enter a website or keyword", Toast.LENGTH_SHORT).show()
                KeywordManager.addKeyword(this, keyword) -> {
                    editTextKeyword.text.clear()
                    loadKeywords()
                    Toast.makeText(this, "\"${keyword.trim()}\" added!", Toast.LENGTH_SHORT).show()
                }
                else ->
                    Toast.makeText(this, "Already in the block list!", Toast.LENGTH_SHORT).show()
            }
        }

        btnEnableService.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            Toast.makeText(this, "Find 'SiteBlocker' and turn it ON", Toast.LENGTH_LONG).show()
        }

        loadKeywords()
    }

    override fun onResume() {
        super.onResume()
        updateServiceStatus()
    }

    private fun updateServiceStatus() {
        if (isAccessibilityServiceEnabled()) {
            statusCard.setCardBackgroundColor(getColor(R.color.status_active))
            tvStatus.text = "✅  Blocker is ACTIVE"
            btnEnableService.visibility = View.GONE
        } else {
            statusCard.setCardBackgroundColor(getColor(R.color.status_inactive))
            tvStatus.text = "⚠️  Blocker is OFF — tap below to enable"
            btnEnableService.visibility = View.VISIBLE
        }
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val am = getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabled = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        return enabled.any { it.resolveInfo.serviceInfo.packageName == packageName }
    }

    private fun loadKeywords() {
        keywords.clear()
        keywords.addAll(KeywordManager.getKeywords(this).sorted())
        adapter.notifyDataSetChanged()
        tvEmpty.visibility = if (keywords.isEmpty()) View.VISIBLE else View.GONE
    }
}

class KeywordAdapter(
    private val items: List<String>,
    private val onDelete: (String) -> Unit
) : RecyclerView.Adapter<KeywordAdapter.VH>() {

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val tvKeyword: TextView    = view.findViewById(R.id.tvKeyword)
        val btnDelete: ImageButton = view.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_keyword, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val keyword = items[position]
        holder.tvKeyword.text = keyword
        holder.btnDelete.setOnClickListener { onDelete(keyword) }
    }

    override fun getItemCount() = items.size
}
