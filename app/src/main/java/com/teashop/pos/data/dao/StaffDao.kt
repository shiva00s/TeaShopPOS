package com.teashop.pos.data.dao

import androidx.room.*
import com.teashop.pos.data.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface StaffDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEmployee(employee: Employee)

    @Query("SELECT * FROM employees WHERE shopId = :shopId AND isActive = 1")
    fun getShopEmployees(shopId: String): Flow<List<Employee>>

    @Insert
    suspend fun insertAttendance(attendance: Attendance)

    @Update
    suspend fun updateAttendance(attendance: Attendance)

    @Query("SELECT * FROM attendance WHERE employeeId = :employeeId ORDER BY checkInTime DESC LIMIT 1")
    suspend fun getLastAttendance(employeeId: String): Attendance?

    @Query("SELECT * FROM attendance WHERE employeeId = :employeeId AND checkInTime >= :start AND checkInTime <= :end")
    suspend fun getAttendanceForPeriod(employeeId: String, start: Long, end: Long): List<Attendance>

    @Insert
    suspend fun insertAdvance(advance: AdvancePayment)

    @Query("SELECT SUM(amount) FROM advance_payments WHERE employeeId = :employeeId AND isRecovered = 0")
    suspend fun getPendingAdvance(employeeId: String): Double?

    @Query("UPDATE advance_payments SET isRecovered = 1, recoveryPaymentId = :paymentId WHERE employeeId = :employeeId AND isRecovered = 0")
    suspend fun markAdvanceRecovered(employeeId: String, paymentId: String)

    @Insert
    suspend fun insertSalaryPayment(payment: SalaryPayment)

    @Insert
    suspend fun insertCashbookEntry(entry: Cashbook)

    @Transaction
    suspend fun processSalary(payment: SalaryPayment, cashbookEntry: Cashbook, recoverAdvance: Boolean) {
        insertSalaryPayment(payment)
        insertCashbookEntry(cashbookEntry)
        if (recoverAdvance) {
            markAdvanceRecovered(payment.employeeId, payment.paymentId)
        }
    }
}
