package com.template.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.template.states.IOUState;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.WireTransaction;

/**
 * Responder flow for the IOU issuance.
 */
@InitiatedBy(Initiator.class)
public class Responder extends FlowLogic<SignedTransaction> {
    private final FlowSession counterpartySession;

    public Responder(FlowSession counterpartySession) {
        this.counterpartySession = counterpartySession;
    }

    @Suspendable
    @Override
    public SignedTransaction call() throws FlowException {
        // Step 1: Sign the transaction after verification
        SignedTransaction signedTx = subFlow(new SignTransactionFlow(counterpartySession) {
            @Override
            protected void checkTransaction(SignedTransaction stx) throws FlowException {
                // Extract the output state
                WireTransaction wireTx = stx.getTx();
                IOUState outputState = wireTx.outputsOfType(IOUState.class).get(0);

                // Ensure that the borrower is the recipient in this transaction
                Party borrower = getOurIdentity();
                if (!outputState.getBorrower().equals(borrower)) {
                    throw new FlowException("This transaction is not meant for this borrower.");
                }
            }
        });

        // Step 2: Record the transaction in both nodes
        return subFlow(new ReceiveFinalityFlow(counterpartySession, signedTx.getId()));
    }
}
