package com.example.paisapet.model

/** UNTAGGED and MISC count against the budget; ESSENTIAL never does. */
enum class Category { MISC, ESSENTIAL, UNTAGGED }

enum class Source { SMS, MANUAL }

/** Where the category was last set. */
enum class TagSource { NOTIFICATION, APP }
