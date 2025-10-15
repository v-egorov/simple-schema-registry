#!/usr/bin/env python3
"""
Large Investment Publications Test Data Generator

This script generates large JSON test files conforming to the invest-publications.schema.json
schema for stress-testing the schema registry with large JSON documents containing embedded
base64 images.

Usage:
    python3 generate-large-investment-publications.py [options]

Options:
    --size SIZE          Target file size: 1mb, 5mb, 10mb, 15mb (default: 1mb)
    --output FILE        Output filename (default: large-publication-{size}.json)
    --schema FILE        Schema file path (default: tests/examples/investment-research/publications/invest-publications.schema.json)
    --validate           Validate generated JSON against schema (requires ajv-cli)
    --help               Show this help message

Examples:
    # Generate 1MB test file
    python3 generate-large-investment-publications.py --size 1mb

    # Generate 10MB test file with validation
    python3 generate-large-investment-publications.py --size 10mb --validate

    # Generate custom sized file
    python3 generate-large-investment-publications.py --size 5mb --output my-test-data.json

Generated files contain:
- Complete hierarchical structure: Publications → Chapters → Blocks → Views
- All optional fields populated with realistic investment/financial content
- Multiple base64-encoded images simulating large binary content
- Comprehensive metadata and notes at all levels
- All supported view types: text, image, chart, table
"""

import json
import random
import string
import argparse
import sys
import os
from datetime import datetime, timedelta
import subprocess

class InvestmentPublicationsGenerator:
    """Generator for large investment publications test data"""

    def __init__(self):
        # Configuration for different file sizes
        self.size_configs = {
            '1mb': {
                'chapters': 3,
                'blocks_per_chapter': 5,
                'base64_multiplier': 15,  # ~15KB base64 per image
                'image_count': 6
            },
            '5mb': {
                'chapters': 6,
                'blocks_per_chapter': 7,
                'base64_multiplier': 40,  # ~40KB base64 per image
                'image_count': 12
            },
            '10mb': {
                'chapters': 8,
                'blocks_per_chapter': 9,
                'base64_multiplier': 80,  # ~80KB base64 per image
                'image_count': 20
            },
            '15mb': {
                'chapters': 10,
                'blocks_per_chapter': 11,
                'base64_multiplier': 120,  # ~120KB base64 per image
                'image_count': 25
            }
        }

        # Investment/financial terminology for realistic content
        self.investment_terms = [
            "market", "equity", "bond", "commodity", "portfolio", "diversification", "risk",
            "return", "volatility", "liquidity", "capital", "investment", "strategy", "analysis",
            "valuation", "earnings", "revenue", "profit", "margin", "growth", "trend", "momentum",
            "technical", "fundamental", "quantitative", "qualitative", "sector", "industry", "global",
            "emerging", "developed", "macroeconomic", "microeconomic", "inflation", "deflation",
            "monetary", "fiscal", "policy", "regulation", "compliance", "governance", "sustainability",
            "impact", "ESG", "carbon", "renewable", "technology", "innovation", "disruption", "digital",
            "transformation", "automation", "artificial", "intelligence", "machine", "learning", "data",
            "analytics", "blockchain", "cryptocurrency", "fintech", "regtech", "wealth", "management",
            "retirement", "planning", "tax", "optimization", "estate", "insurance", "derivative", "option",
            "future", "swap", "structured", "product", "convertible", "preferred", "warrant", "right",
            "offering", "private", "placement", "IPO", "secondary", "merger", "acquisition", "LBO",
            "divestiture", "spin-off", "restructuring", "bankruptcy", "distressed", "credit", "rating",
            "agency", "CDS", "CDO", "MBS", "ABS", "commercial", "paper", "treasury", "municipal",
            "corporate", "high-yield", "investment-grade", "sovereign", "foreign", "exchange", "hedging",
            "interest", "rate", "duration", "convexity", "yield", "curve", "term", "structure", "forward",
            "rate", "swap", "rate", "LIBOR", "SOFR", "fed", "funds", "prime", "discount", "reserve",
            "quantitative", "easing", "tapering", "normalization", "tightening", "easing"
        ]

    def generate_random_string(self, length, chars=string.ascii_letters + string.digits + " "):
        """Generate a random string of given length"""
        return ''.join(random.choice(chars) for _ in range(length))

    def generate_lorem_ipsum_investment(self, length):
        """Generate lorem ipsum text with investment/financial terminology"""
        result = []
        while len(' '.join(result)) < length:
            result.append(random.choice(self.investment_terms))
        return ' '.join(result)[:length]

    def generate_timestamp(self, base_date=None, days_offset=0):
        """Generate ISO timestamp"""
        if base_date is None:
            base_date = datetime.now()
        date = base_date + timedelta(days=days_offset)
        return date.isoformat() + 'Z'

    def generate_base64_content(self, multiplier):
        """Generate base64-encoded content of specified size"""
        # Base64 encoding: 4 chars per 3 bytes, so multiplier * 1024 chars ≈ multiplier * 768 bytes
        length = multiplier * 1024
        base64_chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"

        result = ""
        for i in range(length // len(base64_chars)):
            result += base64_chars
        result += base64_chars[:length % len(base64_chars)]

        # Ensure proper base64 padding
        while len(result) % 4 != 0:
            result += "="

        return result

    def generate_metadata(self):
        """Generate comprehensive metadata"""
        return {
            "assetClasses": ["Equities", "Bonds", "Commodities", "REITs", "FX", "Derivatives", "Alternatives", "Cash"],
            "companies": ["Apple Inc.", "Microsoft Corporation", "Amazon.com Inc.", "Alphabet Inc.", "Tesla Inc.",
                         "JPMorgan Chase & Co.", "Johnson & Johnson", "Procter & Gamble", "Coca-Cola Company", "Walmart Inc.",
                         "Exxon Mobil Corporation", "Chevron Corporation", "Pfizer Inc.", "Verizon Communications Inc."],
            "instruments": ["Stocks", "Bonds", "ETF", "Options", "Futures", "Funds", "Notes", "Swaps"],
            "sectors": ["Energy", "Materials", "Industrials", "Consumer Discretionary", "Consumer Staples",
                       "Health Care", "Financials", "Information Technology", "Communication Services", "Utilities", "Real Estate"],
            "regions": ["Global", "US", "Europe", "APAC", "EMEA", "LATAM", "Emerging Markets", "China", "Japan"],
            "riskProfile": random.choice(["Conservative", "Moderate", "Aggressive"]),
            "timeHorizon": random.choice(["Short-term", "Medium-term", "Long-term"]),
        "tags": random.sample(["market-analysis", "investment-strategy", "risk-management", "portfolio-optimization",
                "economic-indicators", "sector-rotation", "asset-allocation", "diversification",
                "market-timing", "fundamental-analysis", "technical-analysis", "quantitative-methods",
                "behavioral-finance", "sustainable-investing", "impact-investing", "alternative-investments",
                "private-equity", "venture-capital", "hedge-funds", "real-estate", "commodities", "currencies",
                "fixed-income", "equity-markets", "emerging-markets", "developed-markets", "frontier-markets",
                "global-macro", "geopolitical-risk", "regulatory-changes", "monetary-policy", "fiscal-policy",
                "inflation", "deflation", "recession", "expansion", "bull-market", "bear-market", "volatility",
                "liquidity", "correlation", "beta", "alpha", "sharpe-ratio", "sortino-ratio", "maximum-drawdown",
                "value-at-risk", "expected-shortfall", "stress-testing", "scenario-analysis", "monte-carlo-simulation",
                "black-scholes", "binomial-model", "stochastic-calculus", "machine-learning", "artificial-intelligence",
                "big-data", "blockchain", "cryptocurrency", "fintech", "regtech", "robo-advisors", "wealth-management",
                "retirement-planning", "tax-optimization", "estate-planning", "insurance", "derivatives",
                "options-strategies", "futures-contracts", "swaps", "structured-products", "convertible-bonds",
                "preferred-stock", "warrants", "rights-offerings", "private-placements", "initial-public-offerings",
                "secondary-offerings", "mergers-acquisitions", "leveraged-buyouts", "divestitures", "spin-offs",
                "restructuring", "bankruptcy", "distressed-debt", "credit-analysis", "rating-agencies",
                "credit-default-swaps", "collateralized-debt-obligations", "mortgage-backed-securities",
                "asset-backed-securities", "commercial-paper", "treasury-securities", "municipal-bonds",
                "corporate-bonds", "high-yield-bonds", "investment-grade", "sovereign-debt", "emerging-market-debt",
                "foreign-exchange", "currency-hedging", "interest-rate-risk", "duration", "convexity", "yield-curve",
                "term-structure", "forward-rates", "swap-rates", "libor", "sofr", "fed-funds-rate", "prime-rate",
                "discount-rate", "reserve-requirements", "quantitative-easing", "tapering", "normalization",
                "policy-tightening", "policy-easing"], random.randint(1, 100))
        }

    def generate_notes(self, level, count=3):
        """Generate notes for a given level"""
        statuses = ["open", "resolved", "closed"]
        roles = ["author", "editor", "reviewer", "lead_author", "contributor"]

        notes = []
        for i in range(count):
            notes.append({
                "id": f"note_{level}_{i+1:03d}",
                "author": f"Author {random.randint(1, 5)}",
                "role": random.choice(roles),
                "timestamp": self.generate_timestamp(days_offset=random.randint(-30, 0)),
                "status": random.choice(statuses),
                "text": self.generate_lorem_ipsum_investment(200)
            })
        return notes

    def generate_view(self, view_type, image_index=None, base64_multiplier=None):
        """Generate a single view"""
        if view_type == "text":
            return {
                "type": "text",
                "content": f"<p>{self.generate_lorem_ipsum_investment(2000)}</p><p>{self.generate_lorem_ipsum_investment(2000)}</p>"
            }
        elif view_type == "image" and base64_multiplier:
            return {
                "type": "image",
                "content": f"Chart showing {self.generate_lorem_ipsum_investment(100)}",
                "mediaType": "image/png",
                "base64": self.generate_base64_content(base64_multiplier),
                "alt": f"Investment chart {image_index}",
                "caption": f"Figure {image_index}: {self.generate_lorem_ipsum_investment(150)}"
            }
        elif view_type == "chart":
            return {
                "type": "chart",
                "spec": {
                    "type": "line",
                    "data": {
                        "values": [
                            {"date": f"2024-{m:02d}", "value": random.uniform(100, 200), "benchmark": random.uniform(95, 195)}
                            for m in range(1, 13)
                        ]
                    },
                    "encoding": {
                        "x": {"field": "date", "type": "temporal"},
                        "y": {"field": "value", "type": "quantitative"},
                        "color": {"field": "series", "type": "nominal"}
                    }
                }
            }
        elif view_type == "table":
            return {
                "type": "table",
                "columns": [
                    {"key": "metric", "title": "Metric", "type": "string"},
                    {"key": "q1", "title": "Q1 2024", "type": "number", "format": ".2f"},
                    {"key": "q2", "title": "Q2 2024", "type": "number", "format": ".2f"},
                    {"key": "q3", "title": "Q3 2024", "type": "number", "format": ".2f"},
                    {"key": "q4", "title": "Q4 2024", "type": "number", "format": ".2f"}
                ],
                "rows": [
                    {"metric": "Revenue Growth", "q1": random.uniform(-5, 15), "q2": random.uniform(-5, 15),
                     "q3": random.uniform(-5, 15), "q4": random.uniform(-5, 15)},
                    {"metric": "Profit Margin", "q1": random.uniform(5, 25), "q2": random.uniform(5, 25),
                     "q3": random.uniform(5, 25), "q4": random.uniform(5, 25)},
                    {"metric": "ROE", "q1": random.uniform(8, 20), "q2": random.uniform(8, 20),
                     "q3": random.uniform(8, 20), "q4": random.uniform(8, 20)}
                ]
            }

    def generate_block(self, block_id, title_prefix, view_counts, base64_multiplier=None):
        """Generate a block with views"""
        block = {
            "id": block_id,
            "title": f"{title_prefix} - {self.generate_lorem_ipsum_investment(50)}",
            "subtitle": self.generate_lorem_ipsum_investment(100),
            "summary": self.generate_lorem_ipsum_investment(1500),
            "metadata": self.generate_metadata(),
            "notes": self.generate_notes(f"block_{block_id}", 2),
            "createdAt": self.generate_timestamp(days_offset=random.randint(-60, -30)),
            "updatedAt": self.generate_timestamp(days_offset=random.randint(-7, 0)),
            "authors": [f"Author {i+1}" for i in range(random.randint(1, 3))],
            "language": "en",
            "type": random.choice(["textual", "figure", "table", "composite", "custom"]),
            "views": []
        }

        view_types = ["text", "image", "chart", "table"]
        image_index = 1

        for view_type in view_types:
            count = view_counts.get(view_type, 1)
            for i in range(count):
                if view_type == "image" and base64_multiplier:
                    block["views"].append(self.generate_view(view_type, image_index, base64_multiplier))
                    image_index += 1
                else:
                    block["views"].append(self.generate_view(view_type))

        return block

    def generate_chapter(self, chapter_num, block_counts, base64_multiplier=None):
        """Generate a chapter with blocks"""
        chapter = {
            "id": f"ch_{chapter_num:03d}",
            "title": f"Chapter {chapter_num}: {self.generate_lorem_ipsum_investment(60)}",
            "subtitle": self.generate_lorem_ipsum_investment(120),
            "summary": self.generate_lorem_ipsum_investment(2000),
            "metadata": self.generate_metadata(),
            "notes": self.generate_notes(f"ch_{chapter_num:03d}", 3),
            "createdAt": self.generate_timestamp(days_offset=random.randint(-90, -60)),
            "updatedAt": self.generate_timestamp(days_offset=random.randint(-14, 0)),
            "authors": [f"Author {i+1}" for i in range(random.randint(2, 4))],
            "language": "en",
            "blocks": [],
            "order": chapter_num
        }

        for block_num in range(1, block_counts + 1):
            view_counts = {
                "text": random.randint(1, 2),
                "image": random.randint(1, 2) if base64_multiplier else 0,
                "chart": random.randint(1, 2),
                "table": random.randint(1, 2)
            }
            block = self.generate_block(f"block_{chapter_num:03d}_{block_num:02d}",
                                      f"Block {block_num}", view_counts, base64_multiplier)
            chapter["blocks"].append(block)

        return chapter

    def generate_publication(self, pub_id, chapter_count, blocks_per_chapter, base64_multiplier=None):
        """Generate a complete publication"""
        publication = {
            "id": pub_id,
            "title": f"Comprehensive Investment Research Report - Large Scale Test Data",
            "subtitle": "A comprehensive analysis of market trends and investment opportunities with extensive metadata and content blocks",
            "summary": self.generate_lorem_ipsum_investment(4000),
            "type": random.choice(["quarterly_report", "thematic_report", "strategy_note", "market_commentary", "research_note"]),
            "targetAudience": random.choice(["institutional", "retail", "internal"]),
            "status": "published",
            "version": "1.0.0",
            "language": "en",
            "authors": ["Dr. Sarah Chen", "Michael Rodriguez", "Dr. Elena Petrov", "James Wilson", "Dr. Ahmed Hassan"],
            "createdAt": self.generate_timestamp(days_offset=-120),
            "updatedAt": self.generate_timestamp(days_offset=-1),
            "publishedAt": self.generate_timestamp(days_offset=-1),
            "metadata": self.generate_metadata(),
            "notes": self.generate_notes("pub", 5),
            "chapters": []
        }

        for chapter_num in range(1, chapter_count + 1):
            chapter = self.generate_chapter(chapter_num, blocks_per_chapter, base64_multiplier)
            publication["chapters"].append(chapter)

        return publication

    def generate_test_file(self, size, output_file):
        """Generate a complete test file"""
        if size not in self.size_configs:
            raise ValueError(f"Unsupported size: {size}. Supported sizes: {list(self.size_configs.keys())}")

        config = self.size_configs[size]
        print(f"Generating {output_file} ({size})...")

        publication = self.generate_publication(
            f"large_test_pub_{size}",
            config['chapters'],
            config['blocks_per_chapter'],
            config['base64_multiplier']
        )

        data = {"publications": [publication]}

        with open(output_file, 'w', encoding='utf-8') as f:
            json.dump(data, f, indent=2, ensure_ascii=False)

        print(f"Generated {output_file}")

    def validate_json(self, json_file, schema_file):
        """Validate JSON against schema using ajv-cli"""
        try:
            result = subprocess.run(
                ['ajv', 'validate', '-s', schema_file, '-d', json_file],
                capture_output=True,
                text=True,
                cwd=os.path.dirname(json_file)
            )
            if result.returncode == 0:
                print(f"✅ JSON validation passed for {json_file}")
                return True
            else:
                print(f"❌ JSON validation failed for {json_file}")
                print(result.stderr)
                return False
        except FileNotFoundError:
            print("⚠️  ajv-cli not found. Install with: npm install -g ajv-cli")
            return False

def main():
    parser = argparse.ArgumentParser(
        description="Generate large investment publications test data",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog=__doc__
    )

    parser.add_argument(
        '--size',
        choices=['1mb', '5mb', '10mb', '15mb'],
        default='1mb',
        help='Target file size (default: 1mb)'
    )

    parser.add_argument(
        '--output',
        help='Output filename (default: large-publication-{size}.json)'
    )

    parser.add_argument(
        '--schema',
        default='tests/examples/investment-research/publications/invest-publications.schema.json',
        help='Schema file path'
    )

    parser.add_argument(
        '--validate',
        action='store_true',
        help='Validate generated JSON against schema'
    )

    args = parser.parse_args()

    # Determine output file
    if args.output:
        output_file = args.output
    else:
        output_file = f"large-publication-{args.size}.json"

    # Make sure output directory exists
    output_dir = os.path.dirname(output_file)
    if output_dir and not os.path.exists(output_dir):
        os.makedirs(output_dir)

    # Generate the test data
    generator = InvestmentPublicationsGenerator()
    generator.generate_test_file(args.size, output_file)

    # Validate if requested
    if args.validate:
        schema_file = args.schema
        if os.path.exists(schema_file):
            generator.validate_json(output_file, schema_file)
        else:
            print(f"⚠️  Schema file not found: {schema_file}")

    # Show file size
    file_size = os.path.getsize(output_file)
    size_mb = file_size / (1024 * 1024)
    print(".2f")

if __name__ == "__main__":
    main()