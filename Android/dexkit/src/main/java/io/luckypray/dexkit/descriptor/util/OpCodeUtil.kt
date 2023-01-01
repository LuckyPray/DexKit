@file:JvmName("OpCodeUtil")

package io.luckypray.dexkit.descriptor.util

private val opFormat = arrayOf(
    "nop",
    "move",
    "move/from16",
    "move/16",
    "move-wide",
    "move-wide/from16",
    "move-wide/16",
    "move-object",
    "move-object/from16",
    "move-object/16",
    "move-result",
    "move-result-wide",
    "move-result-object",
    "move-exception",
    "return-void",
    "return",
    "return-wide",
    "return-object",
    "const/4",
    "const/16",
    "const",
    "const/high16",
    "const-wide/16",
    "const-wide/32",
    "const-wide",
    "const-wide/high16",
    "const-string",
    "const-string/jumbo",
    "const-class",
    "monitor-enter",
    "monitor-exit",
    "check-cast",
    "instance-of",
    "array-length",
    "new-instance",
    "new-array",
    "filled-new-array",
    "filled-new-array/range",
    "fill-array-data",
    "throw",
    "goto",
    "goto/16",
    "goto/32",
    "packed-switch",
    "sparse-switch",
    "cmpl-float",
    "cmpg-float",
    "cmpl-double",
    "cmpg-double",
    "cmp-long",
    "if-eq",
    "if-ne",
    "if-lt",
    "if-ge",
    "if-gt",
    "if-le",
    "if-eqz",
    "if-nez",
    "if-ltz",
    "if-gez",
    "if-gtz",
    "if-lez",
    "unused-3e",
    "unused-3f",
    "unused-40",
    "unused-41",
    "unused-42",
    "unused-43",
    "aget",
    "aget-wide",
    "aget-object",
    "aget-boolean",
    "aget-byte",
    "aget-char",
    "aget-short",
    "aput",
    "aput-wide",
    "aput-object",
    "aput-boolean",
    "aput-byte",
    "aput-char",
    "aput-short",
    "iget",
    "iget-wide",
    "iget-object",
    "iget-boolean",
    "iget-byte",
    "iget-char",
    "iget-short",
    "iput",
    "iput-wide",
    "iput-object",
    "iput-boolean",
    "iput-byte",
    "iput-char",
    "iput-short",
    "sget",
    "sget-wide",
    "sget-object",
    "sget-boolean",
    "sget-byte",
    "sget-char",
    "sget-short",
    "sput",
    "sput-wide",
    "sput-object",
    "sput-boolean",
    "sput-byte",
    "sput-char",
    "sput-short",
    "invoke-virtual",
    "invoke-super",
    "invoke-direct",
    "invoke-static",
    "invoke-interface",
    "return-void-no-barrier",
    "invoke-virtual/range",
    "invoke-super/range",
    "invoke-direct/range",
    "invoke-static/range",
    "invoke-interface/range",
    "unused-79",
    "unused-7a",
    "neg-int",
    "not-int",
    "neg-long",
    "not-long",
    "neg-float",
    "neg-double",
    "int-to-long",
    "int-to-float",
    "int-to-double",
    "long-to-int",
    "long-to-float",
    "long-to-double",
    "float-to-int",
    "float-to-long",
    "float-to-double",
    "double-to-int",
    "double-to-long",
    "double-to-float",
    "int-to-byte",
    "int-to-char",
    "int-to-short",
    "add-int",
    "sub-int",
    "mul-int",
    "div-int",
    "rem-int",
    "and-int",
    "or-int",
    "xor-int",
    "shl-int",
    "shr-int",
    "ushr-int",
    "add-long",
    "sub-long",
    "mul-long",
    "div-long",
    "rem-long",
    "and-long",
    "or-long",
    "xor-long",
    "shl-long",
    "shr-long",
    "ushr-long",
    "add-float",
    "sub-float",
    "mul-float",
    "div-float",
    "rem-float",
    "add-double",
    "sub-double",
    "mul-double",
    "div-double",
    "rem-double",
    "add-int/2addr",
    "sub-int/2addr",
    "mul-int/2addr",
    "div-int/2addr",
    "rem-int/2addr",
    "and-int/2addr",
    "or-int/2addr",
    "xor-int/2addr",
    "shl-int/2addr",
    "shr-int/2addr",
    "ushr-int/2addr",
    "add-long/2addr",
    "sub-long/2addr",
    "mul-long/2addr",
    "div-long/2addr",
    "rem-long/2addr",
    "and-long/2addr",
    "or-long/2addr",
    "xor-long/2addr",
    "shl-long/2addr",
    "shr-long/2addr",
    "ushr-long/2addr",
    "add-float/2addr",
    "sub-float/2addr",
    "mul-float/2addr",
    "div-float/2addr",
    "rem-float/2addr",
    "add-double/2addr",
    "sub-double/2addr",
    "mul-double/2addr",
    "div-double/2addr",
    "rem-double/2addr",
    "add-int/lit16",
    "rsub-int",
    "mul-int/lit16",
    "div-int/lit16",
    "rem-int/lit16",
    "and-int/lit16",
    "or-int/lit16",
    "xor-int/lit16",
    "add-int/lit8",
    "rsub-int/lit8",
    "mul-int/lit8",
    "div-int/lit8",
    "rem-int/lit8",
    "and-int/lit8",
    "or-int/lit8",
    "xor-int/lit8",
    "shl-int/lit8",
    "shr-int/lit8",
    "ushr-int/lit8",
    "iget-quick",
    "iget-wide-quick",
    "iget-object-quick",
    "iput-quick",
    "iput-wide-quick",
    "iput-object-quick",
    "invoke-virtual-quick",
    "invoke-virtual/range-quick",
    "iput-boolean-quick",
    "iput-byte-quick",
    "iput-char-quick",
    "iput-short-quick",
    "iget-boolean-quick",
    "iget-byte-quick",
    "iget-char-quick",
    "iget-short-quick",
    "unused-f3",
    "unused-f4",
    "unused-f5",
    "unused-f6",
    "unused-f7",
    "unused-f8",
    "unused-f9",
    "invoke-polymorphic",
    "invoke-polymorphic/range",
    "invoke-custom",
    "invoke-custom/range",
    "const-method-handle",
    "const-method-type",
)

fun getOpFormat(opcode: Int): String {
    if (opcode < 0 || opcode >= opFormat.size) {
        throw IllegalArgumentException("opcode: ${opcode.toString(16)} is out of range")
    }
    return opFormat[opcode]
}

fun getOpCode(opFormat: String): Int {
    return when (opFormat) {
        "nop" -> 0
        "move" -> 1
        "move/from16" -> 2
        "move/16" -> 3
        "move-wide" -> 4
        "move-wide/from16" -> 5
        "move-wide/16" -> 6
        "move-object" -> 7
        "move-object/from16" -> 8
        "move-object/16" -> 9
        "move-result" -> 10
        "move-result-wide" -> 11
        "move-result-object" -> 12
        "move-exception" -> 13
        "return-void" -> 14
        "return" -> 15
        "return-wide" -> 16
        "return-object" -> 17
        "const/4" -> 18
        "const/16" -> 19
        "const" -> 20
        "const/high16" -> 21
        "const-wide/16" -> 22
        "const-wide/32" -> 23
        "const-wide" -> 24
        "const-wide/high16" -> 25
        "const-string" -> 26
        "const-string/jumbo" -> 27
        "const-class" -> 28
        "monitor-enter" -> 29
        "monitor-exit" -> 30
        "check-cast" -> 31
        "instance-of" -> 32
        "array-length" -> 33
        "new-instance" -> 34
        "new-array" -> 35
        "filled-new-array" -> 36
        "filled-new-array/range" -> 37
        "fill-array-data" -> 38
        "throw" -> 39
        "goto" -> 40
        "goto/16" -> 41
        "goto/32" -> 42
        "packed-switch" -> 43
        "sparse-switch" -> 44
        "cmpl-float" -> 45
        "cmpg-float" -> 46
        "cmpl-double" -> 47
        "cmpg-double" -> 48
        "cmp-long" -> 49
        "if-eq" -> 50
        "if-ne" -> 51
        "if-lt" -> 52
        "if-ge" -> 53
        "if-gt" -> 54
        "if-le" -> 55
        "if-eqz" -> 56
        "if-nez" -> 57
        "if-ltz" -> 58
        "if-gez" -> 59
        "if-gtz" -> 60
        "if-lez" -> 61
        "aget" -> 68
        "aget-wide" -> 69
        "aget-object" -> 70
        "aget-boolean" -> 71
        "aget-byte" -> 72
        "aget-char" -> 73
        "aget-short" -> 74
        "aput" -> 75
        "aput-wide" -> 76
        "aput-object" -> 77
        "aput-boolean" -> 78
        "aput-byte" -> 79
        "aput-char" -> 80
        "aput-short" -> 81
        "iget" -> 82
        "iget-wide" -> 83
        "iget-object" -> 84
        "iget-boolean" -> 85
        "iget-byte" -> 86
        "iget-char" -> 87
        "iget-short" -> 88
        "iput" -> 89
        "iput-wide" -> 90
        "iput-object" -> 91
        "iput-boolean" -> 92
        "iput-byte" -> 93
        "iput-char" -> 94
        "iput-short" -> 95
        "sget" -> 96
        "sget-wide" -> 97
        "sget-object" -> 98
        "sget-boolean" -> 99
        "sget-byte" -> 100
        "sget-char" -> 101
        "sget-short" -> 102
        "sput" -> 103
        "sput-wide" -> 104
        "sput-object" -> 105
        "sput-boolean" -> 106
        "sput-byte" -> 107
        "sput-char" -> 108
        "sput-short" -> 109
        "invoke-virtual" -> 110
        "invoke-super" -> 111
        "invoke-direct" -> 112
        "invoke-static" -> 113
        "invoke-interface" -> 114
        "return-void-no-barrier" -> 115
        "invoke-virtual/range" -> 116
        "invoke-super/range" -> 117
        "invoke-direct/range" -> 118
        "invoke-static/range" -> 119
        "invoke-interface/range" -> 120
        "neg-int" -> 123
        "not-int" -> 124
        "neg-long" -> 125
        "not-long" -> 126
        "neg-float" -> 127
        "neg-double" -> 128
        "int-to-long" -> 129
        "int-to-float" -> 130
        "int-to-double" -> 131
        "long-to-int" -> 132
        "long-to-float" -> 133
        "long-to-double" -> 134
        "float-to-int" -> 135
        "float-to-long" -> 136
        "float-to-double" -> 137
        "double-to-int" -> 138
        "double-to-long" -> 139
        "double-to-float" -> 140
        "int-to-byte" -> 141
        "int-to-char" -> 142
        "int-to-short" -> 143
        "add-int" -> 144
        "sub-int" -> 145
        "mul-int" -> 146
        "div-int" -> 147
        "rem-int" -> 148
        "and-int" -> 149
        "or-int" -> 150
        "xor-int" -> 151
        "shl-int" -> 152
        "shr-int" -> 153
        "ushr-int" -> 154
        "add-long" -> 155
        "sub-long" -> 156
        "mul-long" -> 157
        "div-long" -> 158
        "rem-long" -> 159
        "and-long" -> 160
        "or-long" -> 161
        "xor-long" -> 162
        "shl-long" -> 163
        "shr-long" -> 164
        "ushr-long" -> 165
        "add-float" -> 166
        "sub-float" -> 167
        "mul-float" -> 168
        "div-float" -> 169
        "rem-float" -> 170
        "add-double" -> 171
        "sub-double" -> 172
        "mul-double" -> 173
        "div-double" -> 174
        "rem-double" -> 175
        "add-int/2addr" -> 176
        "sub-int/2addr" -> 177
        "mul-int/2addr" -> 178
        "div-int/2addr" -> 179
        "rem-int/2addr" -> 180
        "and-int/2addr" -> 181
        "or-int/2addr" -> 182
        "xor-int/2addr" -> 183
        "shl-int/2addr" -> 184
        "shr-int/2addr" -> 185
        "ushr-int/2addr" -> 186
        "add-long/2addr" -> 187
        "sub-long/2addr" -> 188
        "mul-long/2addr" -> 189
        "div-long/2addr" -> 190
        "rem-long/2addr" -> 191
        "and-long/2addr" -> 192
        "or-long/2addr" -> 193
        "xor-long/2addr" -> 194
        "shl-long/2addr" -> 195
        "shr-long/2addr" -> 196
        "ushr-long/2addr" -> 197
        "add-float/2addr" -> 198
        "sub-float/2addr" -> 199
        "mul-float/2addr" -> 200
        "div-float/2addr" -> 201
        "rem-float/2addr" -> 202
        "add-double/2addr" -> 203
        "sub-double/2addr" -> 204
        "mul-double/2addr" -> 205
        "div-double/2addr" -> 206
        "rem-double/2addr" -> 207
        "add-int/lit16" -> 208
        "rsub-int" -> 209
        "mul-int/lit16" -> 210
        "div-int/lit16" -> 211
        "rem-int/lit16" -> 212
        "and-int/lit16" -> 213
        "or-int/lit16" -> 214
        "xor-int/lit16" -> 215
        "add-int/lit8" -> 216
        "rsub-int/lit8" -> 217
        "mul-int/lit8" -> 218
        "div-int/lit8" -> 219
        "rem-int/lit8" -> 220
        "and-int/lit8" -> 221
        "or-int/lit8" -> 222
        "xor-int/lit8" -> 223
        "shl-int/lit8" -> 224
        "shr-int/lit8" -> 225
        "ushr-int/lit8" -> 226
        "iget-quick" -> 227
        "iget-wide-quick" -> 228
        "iget-object-quick" -> 229
        "iput-quick" -> 230
        "iput-wide-quick" -> 231
        "iput-object-quick" -> 232
        "invoke-virtual-quick" -> 233
        "invoke-virtual/range-quick" -> 234
        "iput-boolean-quick" -> 235
        "iput-byte-quick" -> 236
        "iput-char-quick" -> 237
        "iput-short-quick" -> 238
        "iget-boolean-quick" -> 239
        "iget-byte-quick" -> 240
        "iget-char-quick" -> 241
        "iget-short-quick" -> 242
        "invoke-polymorphic" -> 250
        "invoke-polymorphic/range" -> 251
        "invoke-custom" -> 252
        "invoke-custom/range" -> 253
        "const-method-handle" -> 254
        "const-method-type" -> 255
        else -> throw IllegalArgumentException("Unknown standard op format: $opFormat")
    }
}