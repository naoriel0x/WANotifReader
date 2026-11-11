package com.devandra.wanotifread

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.provider.Settings
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var prefs: SharedPreferences
    private lateinit var adapter: ArrayAdapter<String>
    private val contacts = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        prefs = getSharedPreferences("wa_reader_prefs", MODE_PRIVATE)

        val switchEnable = findViewById<Switch>(R.id.switchEnable)
        val switchGroups = findViewById<Switch>(R.id.switchGroups)
        val btnAdd = findViewById<Button>(R.id.btnAddContact)
        val btnAccess = findViewById<Button>(R.id.btnNotifAccess)
        val listView = findViewById<ListView>(R.id.listContacts)

        switchEnable.isChecked = prefs.getBoolean("service_enabled", true)
        switchGroups.isChecked = prefs.getBoolean("read_groups", false)

        contacts.addAll(prefs.getStringSet("priority_contacts", emptySet()) ?: emptySet())
        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, contacts)
        listView.adapter = adapter

        switchEnable.setOnCheckedChangeListener { _, checked ->
            prefs.edit().putBoolean("service_enabled", checked).apply()
            val status = if (checked) "Aktif" else "Nonaktif"
            Toast.makeText(this, "Pembaca $status", Toast.LENGTH_SHORT).show()
        }

        switchGroups.setOnCheckedChangeListener { _, checked ->
            prefs.edit().putBoolean("read_groups", checked).apply()
        }

        btnAdd.setOnClickListener { showAddDialog() }

        listView.setOnItemLongClickListener { _, _, pos, _ ->
            showDeleteDialog(pos)
            true
        }

        btnAccess.setOnClickListener {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }
    }

    private fun showAddDialog() {
        val input = EditText(this).apply { hint = "Nama kontak (sesuai di WA)" }
        AlertDialog.Builder(this)
            .setTitle("Tambah Kontak Prioritas")
            .setView(input)
            .setPositiveButton("Tambah") { _, _ ->
                val name = input.text.toString().trim()
                if (name.isNotEmpty() && !contacts.contains(name)) {
                    contacts.add(name)
                    adapter.notifyDataSetChanged()
                    prefs.edit().putStringSet("priority_contacts", contacts.toSet()).apply()
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun showDeleteDialog(pos: Int) {
        AlertDialog.Builder(this)
            .setTitle("Hapus kontak ini?")
            .setMessage(contacts[pos])
            .setPositiveButton("Hapus") { _, _ ->
                contacts.removeAt(pos)
                adapter.notifyDataSetChanged()
                prefs.edit().putStringSet("priority_contacts", contacts.toSet()).apply()
            }
            .setNegativeButton("Batal", null)
            .show()
    }
}