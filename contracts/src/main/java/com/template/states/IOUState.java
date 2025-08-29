package com.template.states;

import com.template.contracts.IOUContract;
import net.corda.core.contracts.BelongsToContract;
import net.corda.core.contracts.ContractState;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

@BelongsToContract(IOUContract.class)
public class IOUState implements ContractState {
    private final int amount;
    private final Party lender;
    private final Party borrower;
    private final UniqueIdentifier linearId;

    public IOUState(int amount, Party lender, Party borrower, UniqueIdentifier linearId) {
        if (amount <= 0) {
            throw new IllegalArgumentException("IOU amount must be positive.");
        }
        if (lender.equals(borrower)) {
            throw new IllegalArgumentException("Lender and borrower cannot be the same entity.");
        }
        this.amount = amount;
        this.lender = lender;
        this.borrower = borrower;
        this.linearId = linearId;
    }

    public int getAmount() {
        return amount;
    }

    public Party getLender() {
        return lender;
    }

    public Party getBorrower() {
        return borrower;
    }

    public UniqueIdentifier getLinearId() {
        return linearId;
    }

    @NotNull
    @Override
    public List<AbstractParty> getParticipants() {
        return Arrays.asList(lender, borrower);
    }

    //  Fixed: Now returns the correct value
    public int getValue() {
        return amount; // Instead of returning 0
    }
}
