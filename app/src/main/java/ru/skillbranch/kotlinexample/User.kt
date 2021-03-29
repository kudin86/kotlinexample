package ru.skillbranch.kotlinexample

import androidx.annotation.VisibleForTesting
import java.lang.StringBuilder
import java.math.BigInteger
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.*

class User private constructor(
    val firstName: String,
    val lastName: String?,
    email: String? = null,
    rawPhone: String? = null,
    meta: Map<String, Any>? = null
){
    val userInfo: String

    val fullName: String
            get() = listOfNotNull(firstName, lastName)
            .joinToString(" ")
            .capitalize()
    val initials: String
        get() = listOfNotNull(firstName, lastName)
            .map { it.first().toUpperCase() }
            .joinToString ( " " )
    var phone: String? = null
        set(value){
            field = value?.replace("""[^+\d]""".toRegex(),"")
        }
    var login: String
    var salt: String? = null
    lateinit var passwordHash: String

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    var accessCode: String? = null

    init {
        println("First init block, primary consrtuctor was colled ")

        check(firstName.isNotBlank()) {"FirstName must not be blank"}
        check(!email.isNullOrBlank() || !rawPhone.isNullOrBlank()) {"Email or phone not be null or blank"}

        phone = rawPhone;
        login = email ?: phone!!
        login = login.toLowerCase(Locale.ROOT);


        userInfo = """
            firstName: $firstName
            lastName: $lastName
            login: $login
            fullName: $fullName
            initials: $initials
            email: $email
            phone: $phone
            meta: $meta
        """.trimIndent()
    }

    //For phone
    constructor(
        firstName: String,
        lastName: String?,
        rawPhone: String?
    ) : this(firstName, lastName, rawPhone = rawPhone, meta = mapOf("auth" to "sms")){
        println("Secondary phone constructor")
        val code = generateAccessCode()
        passwordHash = encrypt(code);
        println("Phone passwordHash is $passwordHash")
        accessCode = code
    }

    //For email
    constructor(
        firstName: String,
        lastName: String?,
        email: String?,
        password: String
    ) : this(firstName, lastName, email = email, meta = mapOf("auth" to "password")){
        println("Secondary email constructor")
        passwordHash = encrypt(password!!);
        println("email passwordHash is $passwordHash")
    }

    //For csv
    constructor(
        firstName: String,
        lastName: String?,
        email: String?,
        rawSalt: String,
        rawPasswordHash: String,
        rawPhone: String?,
        rawMeta: String?

    ) : this(firstName, lastName, email = email,  rawPhone = rawPhone, meta = mapOf("src" to "csv")){
        println("Secondary csv constructor")
        passwordHash = rawPasswordHash;
        salt = rawSalt
        phone = rawPhone
        println("csv passwordHash is $passwordHash")
    }

    private fun encrypt(password: String): String {
        if(salt.isNullOrEmpty()){
            salt = ByteArray(16).also { SecureRandom().nextBytes(it) }.toString();
        }
        println("Salt while encrypt : $salt")
        return salt.plus(password).md5()
    }

    private fun String.md5(): String {
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(toByteArray()) //16 byte
        val hexString = BigInteger(1, digest).toString(16)
        return hexString.padStart(32, '0')
    }

    fun generateAccessCode(): String {
        val possible = "ABCDEFGHIJKLMNOPRSQTUVWXYZabcdefghijklmnoprsqtuvwxyz0123456789"

        return StringBuilder().apply {
            repeat(6) {
                (possible.indices).random().also { index ->
                    append(possible[index])
                }
            }
        }.toString()
    }

    fun checkPassword(pass:String) = encrypt(pass)  == passwordHash.also {
        println("Checking passwordhash is $passwordHash")
    }

    fun requestAccessCode(){
        val code = generateAccessCode()
        passwordHash = encrypt(code);
        println("generateAccessCode passwordHash is $passwordHash")
        accessCode = code
    }


    companion object Factory {
        fun makeUser(

            fullName: String,
            email: String? = null,
            password: String? = null,
            phone: String? = null
        ):User {
            val (firstName,lastName) = fullName.fullNameToPair()

            return when {
                !phone.isNullOrBlank() -> User(firstName, lastName, phone)
                !email.isNullOrBlank() && !password.isNullOrBlank() -> User(
                    firstName,
                    lastName,
                    email,
                    password)
                else -> throw IllegalArgumentException("Email or phone must not be null or blank")
            }
        }

        fun makeUserCsv(
            fullName: String,
            email: String?,
            salt: String,
            hasPasword: String,
            phone: String?,
            meta: String
        ):User {
            val (firstName,lastName) = fullName.fullNameToPair()

            return when {
                //!phone.isNullOrBlank() -> User(firstName, lastName, phone)
                (!email.isNullOrBlank() || !phone.isNullOrBlank()) && !hasPasword.isNullOrBlank() -> User(
                    firstName,
                    lastName,
                    email,
                    salt,
                    hasPasword,
                    phone,
                    meta
                )
                else -> throw IllegalArgumentException("Email or phone must not be null or blank")
            }
        }

        private fun String.fullNameToPair() : Pair<String, String?> =
            this.split(" ")
                .filter { it.isNotBlank() }
                .run {
                    when (size){
                        1 -> first() to null
                        2 -> first() to last()
                        else -> throw IllegalArgumentException("Fullname must contain only first name and last name")
                    }
                }
    }

}
