package kpy.ext

import kpy.wrappers.PyObjectT
import kpy.utilities.toPython
import python.*


operator fun PyObjectT.invoke(args: PyObjectT = null, kwargs: PyObjectT = null) = PyObject_Call(this, args, kwargs)

operator fun PyObjectT.unaryPlus() = PyNumber_Positive(this)
operator fun PyObjectT.unaryMinus() = PyNumber_Negative(this)
operator fun PyObjectT.not() = PyObject_Not(this)

operator fun PyObjectT.plus(other: PyObjectT) = PyNumber_Add(this, other)
operator fun PyObjectT.plus(other: Any) = this.plus(other.toPython())

operator fun PyObjectT.minus(other: PyObjectT) = PyNumber_Subtract(this, other)
operator fun PyObjectT.minus(other: Any) = this.minus(other.toPython())

operator fun PyObjectT.times(other: PyObjectT) = PyNumber_Multiply(this, other)
operator fun PyObjectT.times(other: Any) = this.times(other.toPython())

operator fun PyObjectT.div(other: PyObjectT) = PyNumber_TrueDivide(this, other)
operator fun PyObjectT.div(other: Any) = this.div(other.toPython())

operator fun PyObjectT.rem(other: PyObjectT) = PyNumber_Remainder(this, other)
operator fun PyObjectT.rem(other: Any) = this.rem(other.toPython())

operator fun PyObjectT.get(key: PyObjectT) = PyObject_GetItem(this, key)
operator fun PyObjectT.get(key: Any) = this[key.toPython()]

operator fun PyObjectT.set(key: PyObjectT, value: PyObjectT) = PyObject_SetItem(this, key, value)
operator fun PyObjectT.set(key: PyObjectT, value: Any) = this.set(key, value.toPython())

operator fun PyObjectT.plusAssign(other: PyObjectT) { PyNumber_InPlaceAdd(this, other) }
operator fun PyObjectT.plusAssign(other: Any) = this.plusAssign(other.toPython())

operator fun PyObjectT.minusAssign(other: PyObjectT) { PyNumber_InPlaceSubtract(this, other) }
operator fun PyObjectT.minusAssign(other: Any) = this.minusAssign(other.toPython())

operator fun PyObjectT.timesAssign(other: PyObjectT) { PyNumber_InPlaceMultiply(this, other) }
operator fun PyObjectT.timesAssign(other: Any) = this.timesAssign(other.toPython())

operator fun PyObjectT.divAssign(other: PyObjectT) { PyNumber_InPlaceTrueDivide(this, other) }
operator fun PyObjectT.divAssign(other: Any) = this.divAssign(other.toPython())

operator fun PyObjectT.remAssign(other: PyObjectT) { PyNumber_InPlaceRemainder(this, other) }
operator fun PyObjectT.remAssign(other: Any) = this.remAssign(other.toPython())
