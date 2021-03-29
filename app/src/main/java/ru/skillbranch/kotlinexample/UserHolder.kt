package ru.skillbranch.kotlinexample

import android.provider.ContactsContract
import androidx.annotation.VisibleForTesting
import java.util.*

class UserHolder {
    private val map = mutableMapOf<String, User>()

    fun registerUser(
        fullName: String,
        email: String,
        password: String
    ):User {

        val login : String = email.toLowerCase(Locale.ROOT)
        if (map.containsKey(login)){
            throw IllegalArgumentException("A user with this email already exists")
        }
        val user: User = User.makeUser(fullName, email = email, password = password)
            .also { user -> map[user.login] = user }
        return user
    }

    fun registerUserByPhone(
        fullName: String,
        phone: String
    ):User {

        val login : String = phone.replace("""[^+\d]""".toRegex(),"")
        if (!(login.length == 12 && login[0] == '+') ) {
            throw IllegalArgumentException("Enter a valid phone number starting with a + and containing 11 digits")
        }
        if (map.containsKey(login)){
            throw IllegalArgumentException("A user with this phone already exists")
        }
        val user: User = User.makeUser(fullName, phone = phone)
            .also { user -> map[user.login] = user }
        return user
    }

    fun loginUser(login: String, password: String): String? {

        //login for email
        var user: User?
        val loginEmail = login.trim().toLowerCase();

        user = map[loginEmail];
        if (user == null){
            val loginPhone = login.trim().replace("""[^+\d]""".toRegex(),"");
            user = map[loginPhone];
            }


        if (user == null) return null

        if (user.checkPassword(password)) return user.userInfo
        else return null
    }

    fun requestAccessCode(login: String){
        var user: User?
        val loginPhone = login.trim().replace("""[^+\d]""".toRegex(),"");
        user = map[loginPhone];
        if ( user != null) user.requestAccessCode();

    }

    fun importUsers(list: List<String>) : List<User>?{
        var listUser: MutableList<User> =  mutableListOf();

        list.forEach {
            val listCurrentRecord = it.split(";")
            val fullName = listCurrentRecord[0]
            val email = listCurrentRecord[1]
            val salt = listCurrentRecord[2].split(":")[0]
            val hasPasword = listCurrentRecord[2].split(":")[1]
            val phone = listCurrentRecord[3]
            val meta = listCurrentRecord[4]

            val user = registerUserByCsv(fullName, email, salt, hasPasword, phone, meta)
            listUser.add(user)

        }

        return listUser
    }

    fun registerUserByCsv(
        fullName: String,
        email: String,
        salt: String,
        hasPasword: String,
        phone: String,
        meta: String
    ):User {

        val login : String = email.toLowerCase(Locale.ROOT)
        /*val phoneFormat = phone.replace("""[^+\d]""".toRegex(),"")
        if (!(phoneFormat.length == 12 && phoneFormat[0] == '+') ) {
            throw IllegalArgumentException("Enter a valid phone number starting with a + and containing 11 digits")
        }*/
        //if (map.containsKey(login)){
        //    throw IllegalArgumentException("A user with this phone already exists")
        //}
        val user: User = User.makeUserCsv(fullName, email, salt, hasPasword,  phone, meta)
            .also { user -> map[user.login] = user }
        return user
    }

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    fun clearHolder() {
        map.clear()
    }
}