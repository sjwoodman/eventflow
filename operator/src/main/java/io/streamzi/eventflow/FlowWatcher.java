package io.streamzi.eventflow;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.apiextensions.CustomResourceDefinition;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.Watcher;
import io.streamzi.eventflow.crds.DoneableFlow;
import io.streamzi.eventflow.crds.Flow;
import io.streamzi.eventflow.crds.FlowList;

import java.util.logging.Logger;

public class FlowWatcher implements Watcher<Flow>, Runnable {

    private static final Logger logger = Logger.getLogger(FlowWatcher.class.getName());

    private FlowController controller;

    public FlowWatcher(FlowController controller) {
        this.controller = controller;
    }

    @Override
    public void eventReceived(Action action, Flow flow) {
        final ObjectMeta metadata = flow.getMetadata();

        final String name = metadata.getName();

        logger.fine("Flow watch received event " + action + " on Custom Resource " + name);

        try {
            switch (action) {
                case ADDED:
                    controller.onAdded(flow);
                    break;
                case MODIFIED:
                    controller.onModified(flow);
                    break;
                case DELETED:
                    controller.onDeleted(flow);
                    break;
                case ERROR:
                    logger.warning("Watch received action=ERROR for Flow " + name);
            }
        } catch (Exception e) {
            logger.warning(e.getMessage());
        }
    }


    /**
     * Thread that's running the Flow Watcher
     */
    @Override
    public void run() {
        logger.info("Starting FlowWatcher");

        //todo: check token is valid

        final CustomResourceDefinition flowCRD = ClientCache.getClient().customResourceDefinitions().withName("flows.streamzi.io").get();

        ClientCache.getClient().customResources(flowCRD, Flow.class, FlowList.class, DoneableFlow.class).inNamespace(ClientCache.getClient().getNamespace()).watch(this);
    }

    @Override
    public void onClose(KubernetesClientException e) {
        logger.info("Closing Watcher: " + this);
        logger.info(e.getMessage());
    }

}
