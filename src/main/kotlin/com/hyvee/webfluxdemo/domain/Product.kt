package com.hyvee.webfluxdemo.domain

import javax.validation.constraints.NotEmpty

data class Product(@field:NotEmpty val name: String = "", val upc: String, val price: Double)