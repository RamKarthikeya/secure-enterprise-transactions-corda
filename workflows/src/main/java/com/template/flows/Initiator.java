package com.template.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.template.contracts.IOUContract;
import com.template.states.IOUState;
import net.corda.core.contracts.Command;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;

import java.util.Arrays;
import java.util.Collections;

/**
 * Initiator flow to issue an IOU.
 */
@InitiatingFlow
@StartableByRPC
public class Initiator extends FlowLogic<SignedTransaction> {
    private final int amount;
    private final Party borrower;

    private final ProgressTracker progressTracker = new ProgressTracker();

    public Initiator(int amount, Party borrower) {
        this.amount = amount;
        this.borrower = borrower;
    }

    @Override
    public ProgressTracker getProgressTracker() {
        return progressTracker;
    }

    @Suspendable
    @Override
    public SignedTransaction call() throws FlowException {
        // Get the lender's identity
        Party lender = getOurIdentity();

        if (lender.equals(borrower)) {
            throw new FlowException("Lender and borrower cannot be the same party.");
        }

        // Get the first notary from the network map
        Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);

        // Create the IOU state
        IOUState outputState = new IOUState(amount, lender, borrower, new UniqueIdentifier());

        // Define the command (both lender and borrower must sign)
        Command<IOUContract.Commands.Issue> command =
                new Command<>(new IOUContract.Commands.Issue(), Arrays.asList(lender.getOwningKey(), borrower.getOwningKey()));

        // Build the transaction
        TransactionBuilder txBuilder = new TransactionBuilder(notary)
                .addOutputState(outputState, IOUContract.ID)
                .addCommand(command);

        // Verify the transaction
        txBuilder.verify(getServiceHub());

        // Sign the transaction
        SignedTransaction signedTx = getServiceHub().signInitialTransaction(txBuilder);

        // Initiate session with borrower
        FlowSession borrowerSession = initiateFlow(borrower);

        // Collect borrower's signature
        SignedTransaction fullySignedTx = subFlow(new CollectSignaturesFlow(signedTx, Collections.singletonList(borrowerSession)));

        // Finalize transaction
        return subFlow(new FinalityFlow(fullySignedTx, Collections.singletonList(borrowerSession)));
    }
}
