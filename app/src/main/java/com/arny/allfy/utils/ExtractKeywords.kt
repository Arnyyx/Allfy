import android.content.Context
import java.io.BufferedReader
import java.io.InputStreamReader

object KeywordExtractor {
    private lateinit var stopWords: Set<String>

    // Khởi tạo stop words khi ứng dụng bắt đầu
    fun initialize(context: Context) {
        stopWords = loadStopWords(context)
    }

    fun extractKeywords(text: String, maxKeywords: Int = 20): List<String> {
        if (!::stopWords.isInitialized) {
            throw IllegalStateException("KeywordExtractor must be initialized with a context first!")
        }

        // Tách thành các từ, giữ nguyên dấu
        val words = text.replace(Regex("[^\\p{L}\\s]"), " ") // Chỉ giữ chữ cái và khoảng trắng
            .trim()
            .split("\\s+".toRegex())
            .filter { it.length > 2 }

        // Xác định ngôn ngữ (đơn giản)
        val detectedLanguage = detectLanguage(text)

        // Tùy chỉnh stop words theo ngôn ngữ (nếu cần)
        val languageSpecificStopWords = when (detectedLanguage) {
            "vi" -> stopWords + setOf("và", "của", "là", "có", "được") // Tiếng Việt
            "en" -> stopWords + setOf("the", "and", "for", "with", "is") // Tiếng Anh
            else -> stopWords // Ngôn ngữ khác dùng mặc định
        }

        // Đếm tần suất, giữ nguyên dạng gốc của từ
        val wordFrequency = words.groupingBy { it }
            .eachCount()
            .filter { it.key.isNotBlank() && !languageSpecificStopWords.contains(it.key.lowercase()) }

        // Lấy tối đa maxKeywords từ khóa
        return wordFrequency.entries
            .sortedByDescending { it.value }
            .take(maxKeywords)
            .map { it.key }
    }

    // Đọc stop words từ file assets
    private fun loadStopWords(context: Context): Set<String> {
        return try {
            context.assets.open("stopwords.txt").use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    reader.readLines()
                        .map { it.trim() }
                        .filter { it.isNotEmpty() }
                        .toSet()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptySet()
        }
    }

    // Hàm nhận diện ngôn ngữ đơn giản
    private fun detectLanguage(text: String): String {
        val vietnameseChars =
            Regex("[àáạảãâầấậẩẫăằắặẳẵèéẹẻẽêềếệểễìíịỉĩòóọỏõôồốộổỗơờớợởỡùúụủũưừứựửữỳýỵỷỹđ]")
        val vietnameseCount = vietnameseChars.findAll(text).count()
        val totalLength = text.filter { it.isLetter() }.length

        // Nếu hơn 20% ký tự là đặc trưng của tiếng Việt
        return if (totalLength > 0 && vietnameseCount * 100 / totalLength > 20) {
            "vi"
        } else {
            "en" // Mặc định là tiếng Anh nếu không rõ
        }
    }
}
