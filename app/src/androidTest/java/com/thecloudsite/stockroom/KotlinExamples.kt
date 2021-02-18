import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class KotlinExamples {

  @Test
  @Throws(Exception::class)
  fun arrayTest() {

    val intArray1 = IntArray(5) { -1 }
    val intArray2 = intArrayOf(-1, 0, 1, 2, 3)

    fun getInts(intArray: IntArray): String {
      return intArray.filter { it > 0 }
        .joinToString(
          prefix = "[",
          separator = ",",
          postfix = "]"
        )
    }

    val ints1 = getInts(intArray1)
    assertEquals("[]", ints1)

    val ints2 = getInts(intArray2)
    assertEquals("[1,2,3]", ints2)
  }

  @Test
  @Throws(Exception::class)
  fun pairsTest() {

    data class Pairs(
      var x: Int,
      var y: Int,
    )

    val liste: MutableList<Pairs> = mutableListOf()
    liste.add(Pairs(x = 1, y = 1))

    // item added
    assertEquals(1, liste.size)
  }

  @Test
  @Throws(Exception::class)
  fun conditionalsTest() {

    val str = "test"
    var result: String = "N/A"

    if (str == "test") {
      result = "match"
    } else
      if (str.length > 4) {
        result = "to long"
      } else
        if (str.length < 4) {
          result = "to small"
        }

    when {
      str == "test" -> {
        result = "match"
      }
      str.length > 4 -> {
        result = "to long"
      }
      str.length < 4 -> {
        result = "to small"
      }
    }

    result = when {
      str == "test" -> {
        "match"
      }
      str.length > 4 -> {
        "to long"
      }
      str.length < 4 -> {
        "to small"
      }
      else -> {
        ""
      }
    }

    assertEquals("match", result)
  }

  @Test
  @Throws(Exception::class)
  fun loopTest() {

    val size = 3
    var sum = 0
    for (j in 0..size) {
      sum += j
    }

    // 0+1+2+3
    assertEquals(6, sum)

    sum = 0
    for (j in 0 until size) {
      sum += j
    }

    // 0+1+2
    assertEquals(3, sum)

    val timestamps = intArrayOf(1, 10, 20, 3, 8)
    sum = 0
    // timestamps.indices = 0..4
    for (i in timestamps.indices) {
      sum += timestamps[i]
    }

    assertEquals(42, sum)

    sum = 0
    timestamps.forEachIndexed { i, timestamp ->
      // timestamp == timestamps[i]
      sum += timestamp
    }

    assertEquals(42, sum)

    sum = timestamps.sum()
    assertEquals(42, sum)
  }

  @Test
  @Throws(Exception::class)
  fun mapTest() {

    data class Person(
      var firstName: String,
      var lastName: String,
      var order: Int,
    )

    val nameList = listOf(
      Person(
        firstName = "Andreas",
        lastName = "Schmidt",
        order = 10
      ),
      Person(
        firstName = "Bettina",
        lastName = "Schmidt",
        order = 2
      ),
      Person(
        firstName = "Christian",
        lastName = "Meier",
        order = 1
      )
    )

    val fullNameList = nameList.map { person ->
      "${person.firstName} ${person.lastName}"
    }

    assertEquals(
      listOf("Andreas Schmidt", "Bettina Schmidt", "Christian Meier"),
      fullNameList
    )

    val sortedFullNameList = nameList.sortedBy { person ->
      person.order
    }.map { person ->
      "${person.firstName} ${person.lastName}"
    }

    assertEquals(
      listOf("Christian Meier", "Bettina Schmidt", "Andreas Schmidt"),
      sortedFullNameList
    )
  }

  @Test
  @Throws(Exception::class)
  fun importTextTest() {

    val text: String = "abc,abc,,\"def\";AZN\nline\ttab\rret space,non_A-Z,\t亜,toLongToBeAName"

    val symbols = text.split("[ ,;\r\n\t]".toRegex())

    assertEquals(symbols.size, 13)
  }

  enum class FilterTypeEnum {
    FilterNullType,
    FilterIntType,
    FilterStringType,
  }

  interface IFilterType {
    fun filter(value: Int): Boolean
    fun dataReady()
    val typeId: FilterTypeEnum
    var data: String
  }

  @Test
  @Throws(Exception::class)
  fun classTest() {

    open class FilterBaseType : IFilterType {
      override fun filter(value: Int): Boolean {
        return false
      }

      override fun dataReady() {
      }

      override val typeId = FilterTypeEnum.FilterNullType
      override var data = ""
    }

    open class FilterIntType : FilterBaseType() {

      override fun filter(value: Int): Boolean {
        data = "$value"
        return value == 1
      }
      override val typeId = FilterTypeEnum.FilterIntType
      override var data: String = ""
        set(value) {
          field = "[$value]"
        }
        get() {
          return if (field.isNotEmpty()) {
            "FilterIntTypeData"
          } else {
            "empty"
          }
        }
    }

    val test: FilterIntType = FilterIntType()

    val result = if (test.filter(1)) {
      test.dataReady()
      test.data
    } else {
      ""
    }

    assertEquals("FilterIntTypeData", result)
  }
}
