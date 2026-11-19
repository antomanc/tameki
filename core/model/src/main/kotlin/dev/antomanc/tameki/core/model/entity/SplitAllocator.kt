package dev.antomanc.tameki.core.model.entity

import dev.antomanc.tameki.core.model.Money
import java.math.BigInteger

/**
 * Rescales an existing breakdown to a new total instead of discarding it.
 *
 * A split must always sum to its transaction, and the quick-add sheet has no split editor: when
 * the user edits the amount of a split transaction, the honest move is to keep the categories the
 * user already chose and move each share by the same proportion, then adjust the rounding cents
 * on the largest remainders. The sheet tells the user this happened; it never drops the breakdown
 * behind their back.
 *
 * Whole cents are allocated with the largest-remainder method, so the result sums exactly to
 * [total]. Ties go to the earlier split, which keeps the outcome deterministic.
 */
object SplitAllocator {
    /**
     * @param splits the stored breakdown, empty or (ideally) summing to the old amount.
     * @param total the new amount; its currency becomes each split's currency.
     * @return a breakdown with the same categories, notes and order, summing to [total]. The input
     *   is returned untouched when it is empty, already correct, or the old total is unusable.
     */
    fun scaleTo(splits: List<Split>, total: Money): List<Split> {
        if (splits.isEmpty() || total.minor <= 0L) return splits
        val oldTotal = splits.sumOf { it.amount.minor }
        if (oldTotal <= 0L) return splits
        if (oldTotal == total.minor && splits.all { it.amount.currency == total.currency }) {
            return splits
        }

        val exact = splits.map { split ->
            val numerator = BigInteger.valueOf(split.amount.minor) * BigInteger.valueOf(total.minor)
            val (quotient, remainder) = numerator.divideAndRemainder(BigInteger.valueOf(oldTotal))
            quotient.longValueExact() to remainder.longValueExact()
        }
        val allocated = exact.map { it.first }.toMutableList()
        var missing = total.minor - allocated.sum()
        // Flooring leaves fewer than one cent per split unassigned; give them to the largest
        // remainders, never more than one each.
        val byRemainder = splits.indices.sortedWith(
            compareByDescending<Int> { exact[it].second }.thenBy { it },
        )
        var index = 0
        while (missing > 0L) {
            allocated[byRemainder[index % byRemainder.size]] += 1L
            missing -= 1L
            index += 1
        }
        return splits.mapIndexed { splitIndex, split ->
            split.copy(amount = Money(allocated[splitIndex], total.currency))
        }
    }
}
