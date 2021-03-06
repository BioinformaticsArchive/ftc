values <- read.csv("/home/samuel/git/ftc/data/analysis/atc_moa_sim.csv", head=TRUE, sep=",")
palette <- colorRampPalette(c('#f0f3ff','#0033BB'))
smoothScatter(values, colramp=palette)
abline(h=0.5, v=0.5)


my.symbols(x,y, ms.2circ, inches=0.5, add=FALSE, symb.plots=TRUE, col1="#00ff0088", col2="#ff00ff88")