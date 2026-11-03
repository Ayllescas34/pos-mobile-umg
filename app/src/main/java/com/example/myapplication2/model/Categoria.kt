package com.example.myapplication2.model

import com.google.gson.annotations.SerializedName

data class Categoria(
    @SerializedName("id_categoria")
    val id: Int,

    val nombre: String,
    val descripcion: String?
)
