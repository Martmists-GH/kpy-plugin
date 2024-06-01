package kpy.annotations

// See: https://docs.python.org/3/c-api/typeobj.html
enum class PyMagicMethod {
    // tp
    TP_GETATTRO,
    TP_SETATTRO,
    TP_RICHCOMPARE,
    TP_ITER,
    TP_ITERNEXT,
    TP_TRAVERSE,

    // tp_as_async
    AM_AWAIT,
    AM_AITER,
    AM_ANEXT,

    // tp_as_number
    NB_ABSOLUTE,
    NB_ADD,
    NB_AND,
    NB_BOOL,
    NB_DIVMOD,
    NB_FLOAT,
    NB_FLOOR_DIVIDE,
    NB_INDEX,
    NB_INPLACE_ADD,
    NB_INPLACE_AND,
    NB_INPLACE_FLOOR_DIVIDE,
    NB_INPLACE_LSHIFT,
    NB_INPLACE_MATRIX_MULTIPLY,
    NB_INPLACE_MULTIPLY,
    NB_INPLACE_OR,
    NB_INPLACE_POWER,
    NB_INPLACE_REMAINDER,
    NB_INPLACE_RSHIFT,
    NB_INPLACE_SUBTRACT,
    NB_INPLACE_TRUE_DIVIDE,
    NB_INPLACE_XOR,
    NB_INT,
    NB_INVERT,
    NB_LSHIFT,
    NB_MATRIX_MULTIPLY,
    NB_MULTIPLY,
    NB_NEGATIVE,
    NB_OR,
    NB_POSITIVE,
    NB_POWER,
    NB_REMAINDER,
    NB_RSHIFT,
    NB_SUBTRACT,
    NB_TRUE_DIVIDE,
    NB_XOR,

    // tp_as_sequence
    SQ_LENGTH,
    SQ_CONCAT,
    SQ_REPEAT,
    SQ_ITEM,
    SQ_ASS_ITEM,
    SQ_CONTAINS,
    SQ_INPLACE_CONCAT,
    SQ_INPLACE_REPEAT,

    // tp_as_mapping
    MP_LENGTH,
    MP_SUBSCRIPT,
    MP_ASS_SUBSCRIPT,

    // tp_as_buffer
    BF_GETBUFFER,
    BF_RELEASEBUFFER,
}
