package com.template.contracts;

import net.corda.core.contracts.Command;
import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.Contract;
import net.corda.core.transactions.LedgerTransaction;
import net.corda.core.identity.AbstractParty;
import org.jetbrains.annotations.NotNull;
import com.template.states.IOUState;

import java.security.PublicKey;
import java.util.List;
import java.util.stream.Collectors;

/**
 * IOUContract enforces rules when issuing IOUs.
 */
public class IOUContract implements Contract {
    public static final String ID = "com.template.contracts.IOUContract";

    @Override
    public void verify(@NotNull LedgerTransaction tx) {
        // Ensure that there is exactly one command of type Issue
        Command<Commands.Issue> command = tx.findCommand(
                Commands.Issue.class, cmd -> true);

        // Ensure no inputs are consumed
        if (!tx.getInputStates().isEmpty()) {
            throw new IllegalArgumentException("No input states should be consumed.");
        }

        // Ensure exactly one output state
        if (tx.getOutputStates().size() != 1) {
            throw new IllegalArgumentException("Only one IOU state should be created.");
        }

        // Extract the output state
        IOUState outputState = tx.outputsOfType(IOUState.class).get(0);

        // Ensure the IOU amount is positive
        if (outputState.getAmount() <= 0) {
            throw new IllegalArgumentException("IOU amount must be positive.");
        }

        // Ensure lender and borrower are different entities
        if (outputState.getLender().equals(outputState.getBorrower())) {
            throw new IllegalArgumentException("Lender and borrower must be different.");
        }

        // Ensure that all required signers have signed the transaction
        List<PublicKey> requiredSigners = command.getSigners();
        List<PublicKey> stateParticipants = outputState.getParticipants()
                .stream()
                .map(AbstractParty::getOwningKey)
                .collect(Collectors.toList());

        if (!requiredSigners.containsAll(stateParticipants)) {
            throw new IllegalArgumentException("All participants must sign the transaction.");
        }
    }

    /**
     * Commands for the IOU contract.
     */
    public interface Commands extends CommandData {
        class Issue implements Commands {}  // Issue command
    }
}
