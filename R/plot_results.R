plot_results <- function(file, title, xlab = "Number of vertices", ylab = "Time (s)", ylim = NULL) {

data <- read.csv(paste("results/random/", file, ".csv", sep=""), header = T , sep = ",") 

if (length(ylim)==0) {
  ylim = range(data$SetCoveringTime, data$AssignmentTime, na.rm = TRUE)
}

par(mar = c(3, 3, 1, 0.5), mgp = c(1.8, 0.7, 0))

plot(data$NumVertices, data$SetCoveringTime, type = "o", pch = 0, lty = 1,
  xlab = xlab, ylab = ylab, cex.lab = 0.8, 
  xlim = range(data$NumVertices), 
  ylim = ylim,
  axes = T, cex.axis = 0.8,
  ann = T, lwd = 2)
  

pch = c(0)
lines(data$NumVertices, data$AssignmentTime, type = "o", pch = 1, lty = 1)
pch <- append(pch, 1, after = length(pch))


legend("topleft", c("SetCovering", "Assignment"), cex = 0.8, pch = pch, lty = 1);
title(main = title, cex.main = 0.8)

}