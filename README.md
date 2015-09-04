# SemOpsNeo4j
This program achieves several tasks :
- Read a .csv file or a PostgreSQL database containing images and tags associated
- For each image, 
  1. Check if tags already exist in the specified Neo4j database
  2. Create non-existant tags and their semantic hierarchy
- Depending on the configuration parameters you'll set up this program can also compute a semantic distance matrix between tags.

The *experiment* package contains classes used to find new tags for each image. Several methods are implemented : WholeList, SubLists, DirectNeighbors, WikiLinks, WikiContent. For each image three files will be generated with the results of the launched experiments (CSV, TXT, HTML).
Depending on your parameters, this program can :
- Evaluate the results by removing P% of initial tags and calculate the distance between these and the candidates.
- Generate lots for an user evaluation. Each lot will contain :
	- the images files
	- a HTML file presenting the image, the initial tags and the candidates
	- a CSV file in which the user will rank the candidates

## Context
This project was used for my master thesis, see [this repository](https://github.com/Mogier/master-thesis) for the complete thesis report.

## Configuration
In order to run this programm, you'll need to do the following things :
- Get those programms : 
  1. [TreeGenerator](https://github.com/Mogier/terms-analysis)
  2. [GexfParser](https://github.com/Mogier/GexfParserForNeo4jDB)
- Open all three projects in Eclipse.
- Add TreeGenerator and GexfParser to SemOpsNeo4j's buildpath.
- Make sure you have a Wordnet dictionnary in your computer.
- Make sure you've edited the config.ini file in TreeGenerator repository.
- Edit this project's *config.ini* file with the followings :
	- NEO4JDB_PATH : path to the Neo4j Database repository you'll use.
	- SEPARATOR : character used to join the initial tags' list before using it in TreeGenerator
	- TREE_INI_FILE_PATH : path to the TreeGenerator project's config.ini file
	- GEXF_DIR_PATH : path to the repository in which the potential generated GEXF file should be exported
	- MATRIXES_DIR_PATH : path to the repository in which the potential generated distance matrices files should be exported
	- EXPERIMENTS_DIR_PATH : path to the repository in which the potential generated experiments should be exported
	- IMAGES_PATH : path to the images' repository
- Edit the main class **SemOpsNeo4j.java** (or **RunExperiments.java** if you want to run tests) with your own parameters.
- Run :)