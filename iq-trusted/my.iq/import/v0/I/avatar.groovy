/**
 * Script for handling avatar-related actions within IQ's Symbyotic Cognition operating environment.
 *
 * This script imports necessary dependencies and defines logic for interacting with avatars, 
 * such as performing web searches and processing search results.
 *
 * The script assumes the presence of a `IQFacade` instance for interacting with the IQ API.
 *
 * Usage:
 * - Provide a search query via the `my.prompt` variable.
 * - The script makes a web search using the provided query via the Brave search API.
 * - Results are processed and stored in the `my.results` variable for further usage.
 */
import systems.symbol.agent.IQFacade

// Ensure that a search prompt is provided
if (!my.prompt) return;

// Optionally, cast `IQFacade` and `my` variables to appropriate types for IDE.
iq = (IQFacade)iq;
my = (Map<String,String>)my;

// Make a web search using the Brave search API
def results = iq.api("https://api.search.brave.com/res/v1/web/search","x-subscription-token").get([q: my.prompt ]);

// Curate a collection of search results then store them contextually as my.results
my.results = iq.json(results.body());
