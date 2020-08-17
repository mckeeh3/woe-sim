package woe.simulator;

import java.util.concurrent.CompletionStage;

interface Client {
  CompletionStage<Telemetry.TelemetryResponse> post(Region.SelectionCommand selectionCommand);
}
